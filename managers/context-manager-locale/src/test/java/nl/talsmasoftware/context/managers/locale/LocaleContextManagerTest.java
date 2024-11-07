/*
 * Copyright 2016-2024 Talsma ICT
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
import nl.talsmasoftware.context.core.ContextManagers;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Sjoerd Talsma
 */
public class LocaleContextManagerTest {
    private static final Locale DUTCH = new Locale("nl", "NL");
    private static final Locale ENGLISH = Locale.UK;
    private static final Locale GERMAN = Locale.GERMANY;

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final LocaleContextManager MANAGER = new LocaleContextManager();

    private ExecutorService threadpool;

    @BeforeEach
    public void init() {
        Locale.setDefault(DEFAULT_LOCALE);
        threadpool = new ContextAwareExecutorService(Executors.newCachedThreadPool());
    }

    @AfterEach
    public void cleanup() {
        threadpool.shutdown();
        Locale.setDefault(DEFAULT_LOCALE);
    }

    @Test
    public void testLocalePropagation() throws ExecutionException, InterruptedException {
        try (Context<Locale> ctx1 = LocaleContext.set(DUTCH)) {
            assertThat(ctx1.getValue(), is(DUTCH));
            assertThat(MANAGER.getActiveContextValue(), is(DUTCH));
            assertThat(LocaleContext.getOrDefault(), is(DUTCH));

            final CountDownLatch blocker = new CountDownLatch(1);
            Future<Locale> slowCall;
            try (Context<Locale> ctx2 = MANAGER.initializeNewContext(GERMAN)) {
                assertThat(ctx2.getValue(), is(GERMAN));
                assertThat(MANAGER.getActiveContextValue(), is(GERMAN));
                assertThat(LocaleContext.getOrDefault(), is(GERMAN));

                slowCall = threadpool.submit(() -> {
                    blocker.await(5, TimeUnit.SECONDS);
                    return MANAGER.getActiveContextValue();
                });
            }

            assertThat("Restored context in parent", MANAGER.getActiveContextValue(), is(DUTCH));
            assertThat("Slow thread already done", slowCall.isDone(), is(false));
            blocker.countDown();
            assertThat("Context in slow thread", slowCall.get(), is(GERMAN));
        }
        assertThat("Current context", MANAGER.getActiveContextValue(), is(nullValue()));
        assertThat(LocaleContext.getOrDefault(), is(DEFAULT_LOCALE));
    }

    @Test
    public void testGetCurrentLocaleOrDefault() {
        assertThat(LocaleContext.getOrDefault(), is(DEFAULT_LOCALE));
        try (Context<Locale> ctx1 = LocaleContext.set(GERMAN)) {
            assertThat(LocaleContext.getOrDefault(), is(GERMAN));
            assertThat(MANAGER.getActiveContextValue(), is(GERMAN));

            try (Context<Locale> ctx2 = MANAGER.initializeNewContext(null)) {
                assertThat(LocaleContext.getOrDefault(), is(DEFAULT_LOCALE));
                assertThat(MANAGER.getActiveContextValue(), nullValue());
            } finally {
                assertThat(LocaleContext.getOrDefault(), is(GERMAN));
                assertThat(MANAGER.getActiveContextValue(), is(GERMAN));
            }
        } finally {
            assertThat(LocaleContext.getOrDefault(), is(DEFAULT_LOCALE));
        }
    }

    @Test
    public void testClear() {
        Context<Locale> dutchCtx = MANAGER.initializeNewContext(DUTCH);
        Context<Locale> englishCtx = MANAGER.initializeNewContext(ENGLISH);

        MANAGER.clear();
        assertThat(MANAGER.getActiveContextValue(), is(nullValue()));
        assertThat(LocaleContext.getOrDefault(), is(DEFAULT_LOCALE));

        assertThat(englishCtx.getValue(), equalTo(ENGLISH));
        assertThat(dutchCtx.getValue(), equalTo(DUTCH));
    }

    @Test
    public void testClearActiveContexts() {
        Context<Locale> dutchCtx = MANAGER.initializeNewContext(DUTCH);
        Context<Locale> englishCtx = MANAGER.initializeNewContext(ENGLISH);

        ContextManagers.clearActiveContexts();
        assertThat(MANAGER.getActiveContextValue(), is(nullValue()));
        assertThat(LocaleContext.getOrDefault(), is(DEFAULT_LOCALE));

        assertThat(englishCtx.getValue(), equalTo(ENGLISH));
        assertThat(dutchCtx.getValue(), equalTo(DUTCH));
    }

    @Test
    public void testToString() {
        assertThat(MANAGER, hasToString(containsString("LocaleContextManager")));
    }
}
