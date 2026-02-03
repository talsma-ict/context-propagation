/*
 * Copyright 2016-2026 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.talsmasoftware.context.managers.locale;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.core.concurrent.ContextAwareExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sjoerd Talsma
 */
class CurrentLocaleManagerTest {
    static final Locale DUTCH = Locale.of("nl", "NL");
    static final Locale ENGLISH = Locale.UK;
    static final Locale GERMAN = Locale.GERMANY;

    static final Locale DEFAULT_LOCALE = Locale.getDefault();
    static final CurrentLocaleManager MANAGER = CurrentLocaleManager.provider();

    ExecutorService threadPool;

    @BeforeEach
    void init() {
        Locale.setDefault(DEFAULT_LOCALE);
        threadPool = ContextAwareExecutorService.wrap(Executors.newCachedThreadPool());
    }

    @AfterEach
    void cleanup() {
        threadPool.shutdown();
        Locale.setDefault(DEFAULT_LOCALE);
    }

    @Test
    void activateNull() {
        try (Context context = MANAGER.activate(DUTCH)) {
            assertThat(MANAGER.getActiveContextValue()).isEqualTo(DUTCH);

            try (Context context2 = MANAGER.activate(null)) {
                assertThat(MANAGER.getActiveContextValue()).isNull();

                try (Context context3 = MANAGER.activate(ENGLISH)) {
                    assertThat(MANAGER.getActiveContextValue()).isEqualTo(ENGLISH);
                }

                assertThat(MANAGER.getActiveContextValue()).isNull();
            }

            assertThat(MANAGER.getActiveContextValue()).isEqualTo(DUTCH);
        }
    }

    @Test
    void testLocalePropagation() throws ExecutionException, InterruptedException {
        try (Context ignored = CurrentLocaleHolder.set(DUTCH)) {
            assertThat(MANAGER.getActiveContextValue()).isEqualTo(DUTCH);
            assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(DUTCH);

            final CountDownLatch blocker = new CountDownLatch(1);
            Future<Locale> slowCall;
            try (Context ignored2 = MANAGER.activate(GERMAN)) {
                assertThat(MANAGER.getActiveContextValue()).isEqualTo(GERMAN);
                assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(GERMAN);

                slowCall = threadPool.submit(() -> {
                    assertThat(blocker.await(5, TimeUnit.SECONDS)).isTrue();
                    return MANAGER.getActiveContextValue();
                });
            }

            assertThat(MANAGER.getActiveContextValue()).as("Restored context in parent").isEqualTo(DUTCH);
            assertThat(slowCall.isDone()).as("Slow thread already done").isFalse();
            blocker.countDown();
            assertThat(slowCall.get()).as("Context in slow thread").isEqualTo(GERMAN);
        }
        assertThat(MANAGER.getActiveContextValue()).as("Current context").isNull();
        assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(DEFAULT_LOCALE);
    }

    @Test
    void testGetCurrentLocaleOrDefault() {
        assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(DEFAULT_LOCALE);
        try (Context ignored = CurrentLocaleHolder.set(GERMAN)) {
            assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(GERMAN);
            assertThat(MANAGER.getActiveContextValue()).isEqualTo(GERMAN);

            try (Context ignored2 = MANAGER.activate(null)) {
                assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(DEFAULT_LOCALE);
                assertThat(MANAGER.getActiveContextValue()).isNull();
            } finally {
                assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(GERMAN);
                assertThat(MANAGER.getActiveContextValue()).isEqualTo(GERMAN);
            }
        } finally {
            assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(DEFAULT_LOCALE);
        }
    }

    @Test
    void testClear() {
        MANAGER.activate(DUTCH);
        assertThat(MANAGER.getActiveContextValue()).isEqualTo(DUTCH);
        MANAGER.activate(ENGLISH);
        assertThat(MANAGER.getActiveContextValue()).isEqualTo(ENGLISH);

        MANAGER.clear();
        assertThat(MANAGER.getActiveContextValue()).isNull();
        assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(DEFAULT_LOCALE);
    }

    @Test
    void testClearAll() {
        MANAGER.activate(DUTCH);
        assertThat(MANAGER.getActiveContextValue()).isEqualTo(DUTCH);
        MANAGER.activate(ENGLISH);
        assertThat(MANAGER.getActiveContextValue()).isEqualTo(ENGLISH);

        ContextManager.clearAll();
        assertThat(MANAGER.getActiveContextValue()).isNull();
        assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(DEFAULT_LOCALE);
    }

    @Test
    void testToString() {
        assertThat(MANAGER.toString()).contains("CurrentLocaleManager");
    }
}
