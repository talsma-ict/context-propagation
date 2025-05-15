/*
 * Copyright 2016-2025 Talsma ICT
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Sjoerd Talsma
 */
class CurrentLocaleManagerTest {
    static final Locale DUTCH = new Locale("nl", "NL");
    static final Locale ENGLISH = Locale.UK;
    static final Locale GERMAN = Locale.GERMANY;

    static final Locale DEFAULT_LOCALE = Locale.getDefault();
    static final CurrentLocaleManager MANAGER = CurrentLocaleManager.provider();

    ExecutorService threadPool;

    @BeforeEach
    void init() {
        Locale.setDefault(DEFAULT_LOCALE);
        threadPool = new ContextAwareExecutorService(Executors.newCachedThreadPool());
    }

    @AfterEach
    void cleanup() {
        threadPool.shutdown();
        Locale.setDefault(DEFAULT_LOCALE);
    }

    @Test
    void testLocalePropagation() throws ExecutionException, InterruptedException {
        try (Context ignored = CurrentLocaleHolder.set(DUTCH)) {
            assertThat(MANAGER.getActiveContextValue(), is(DUTCH));
            assertThat(CurrentLocaleHolder.getOrDefault(), is(DUTCH));

            final CountDownLatch blocker = new CountDownLatch(1);
            Future<Locale> slowCall;
            try (Context ignored2 = MANAGER.initializeNewContext(GERMAN)) {
                assertThat(MANAGER.getActiveContextValue(), is(GERMAN));
                assertThat(CurrentLocaleHolder.getOrDefault(), is(GERMAN));

                slowCall = threadPool.submit(() -> {
                    assertThat(blocker.await(5, TimeUnit.SECONDS), is(true));
                    return MANAGER.getActiveContextValue();
                });
            }

            assertThat("Restored context in parent", MANAGER.getActiveContextValue(), is(DUTCH));
            assertThat("Slow thread already done", slowCall.isDone(), is(false));
            blocker.countDown();
            assertThat("Context in slow thread", slowCall.get(), is(GERMAN));
        }
        assertThat("Current context", MANAGER.getActiveContextValue(), is(nullValue()));
        assertThat(CurrentLocaleHolder.getOrDefault(), is(DEFAULT_LOCALE));
    }

    @Test
    void testGetCurrentLocaleOrDefault() {
        assertThat(CurrentLocaleHolder.getOrDefault(), is(DEFAULT_LOCALE));
        try (Context ignored = CurrentLocaleHolder.set(GERMAN)) {
            assertThat(CurrentLocaleHolder.getOrDefault(), is(GERMAN));
            assertThat(MANAGER.getActiveContextValue(), is(GERMAN));

            try (Context ignored2 = MANAGER.initializeNewContext(null)) {
                assertThat(CurrentLocaleHolder.getOrDefault(), is(DEFAULT_LOCALE));
                assertThat(MANAGER.getActiveContextValue(), nullValue());
            } finally {
                assertThat(CurrentLocaleHolder.getOrDefault(), is(GERMAN));
                assertThat(MANAGER.getActiveContextValue(), is(GERMAN));
            }
        } finally {
            assertThat(CurrentLocaleHolder.getOrDefault(), is(DEFAULT_LOCALE));
        }
    }

    @Test
    void testClear() {
        MANAGER.initializeNewContext(DUTCH);
        assertThat(MANAGER.getActiveContextValue(), is(DUTCH));
        MANAGER.initializeNewContext(ENGLISH);
        assertThat(MANAGER.getActiveContextValue(), is(ENGLISH));

        MANAGER.clear();
        assertThat(MANAGER.getActiveContextValue(), is(nullValue()));
        assertThat(CurrentLocaleHolder.getOrDefault(), is(DEFAULT_LOCALE));
    }

    @Test
    void testClearAll() {
        MANAGER.initializeNewContext(DUTCH);
        assertThat(MANAGER.getActiveContextValue(), is(DUTCH));
        MANAGER.initializeNewContext(ENGLISH);
        assertThat(MANAGER.getActiveContextValue(), is(ENGLISH));

        ContextManager.clearAll();
        assertThat(MANAGER.getActiveContextValue(), is(nullValue()));
        assertThat(CurrentLocaleHolder.getOrDefault(), is(DEFAULT_LOCALE));
    }

    @Test
    void testToString() {
        assertThat(MANAGER, hasToString(containsString("CurrentLocaleManager")));
    }
}
