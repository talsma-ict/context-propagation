/*
 * Copyright 2016-2020 Talsma ICT
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
package nl.talsmasoftware.context.locale;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.executors.ContextAwareExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.concurrent.Callable;
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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Sjoerd Talsma
 */
public class LocaleContextManagerTest {
    private static final Locale DUTCH = new Locale("nl", "NL");
    private static final Locale ENGLISH = Locale.UK;
    private static final Locale GERMAN = Locale.GERMANY;

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static LocaleContextManager manager = new LocaleContextManager();
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
        Context<Locale> ctx1 = manager.initializeNewContext(DUTCH);
        try {
            assertThat("Current context", manager.getActiveContext(), is(notNullValue()));
            assertThat("Current context value", manager.getActiveContext().getValue(), is(DUTCH));

            Context<Locale> ctx2 = manager.initializeNewContext(GERMAN);
            final CountDownLatch blocker = new CountDownLatch(1);
            Future<Locale> slowCall;
            try {

                slowCall = threadpool.submit(new Callable<Locale>() {
                    public Locale call() throws InterruptedException {
                        blocker.await(5, TimeUnit.SECONDS);
                        return LocaleContextManager.getCurrentLocale();
                    }
                });

            } finally {
                ctx2.close();
            }
            assertThat("Restored context in parent", LocaleContextManager.getCurrentLocale(), is(DUTCH));
            assertThat("Slow thread already done", slowCall.isDone(), is(false));
            blocker.countDown();
            assertThat("Context in slow thread", slowCall.get(), is(GERMAN));

        } finally {
            ctx1.close();
        }
        assertThat("Current context", manager.getActiveContext(), is(nullValue()));
    }

    @Test
    public void testGetCurrentLocaleOrDefault() {
        assertThat(LocaleContextManager.getCurrentLocaleOrDefault(), is(DEFAULT_LOCALE));
        Context<Locale> ctx1 = manager.initializeNewContext(GERMAN);
        try {
            assertThat(LocaleContextManager.getCurrentLocaleOrDefault(), is(GERMAN));
            Context<Locale> ctx2 = manager.initializeNewContext(null);
            try {
                assertThat(LocaleContextManager.getCurrentLocaleOrDefault(), is(DEFAULT_LOCALE));
            } finally {
                ctx2.close();
                assertThat(LocaleContextManager.getCurrentLocaleOrDefault(), is(GERMAN));
            }
        } finally {
            ctx1.close();
            assertThat(LocaleContextManager.getCurrentLocaleOrDefault(), is(DEFAULT_LOCALE));
        }
    }

    @Test
    public void testClear() {
        Context<Locale> dutchCtx = manager.initializeNewContext(DUTCH);
        Context<Locale> englishCtx = manager.initializeNewContext(ENGLISH);

        LocaleContextManager.clear();
        assertThat(manager.getActiveContext(), is(nullValue()));
        assertThat(LocaleContextManager.getCurrentLocale(), is(nullValue()));

        assertThat(((LocaleContext) englishCtx).isClosed(), is(true));
        assertThat(((LocaleContext) dutchCtx).isClosed(), is(true));
        assertThat(englishCtx.getValue(), equalTo(ENGLISH));
        assertThat(dutchCtx.getValue(), equalTo(DUTCH));
    }

    @Test
    public void testClearActiveContexts() {
        Context<Locale> dutchCtx = manager.initializeNewContext(DUTCH);
        Context<Locale> englishCtx = manager.initializeNewContext(ENGLISH);

        ContextManagers.clearActiveContexts();
        assertThat(manager.getActiveContext(), is(nullValue()));
        assertThat(LocaleContextManager.getCurrentLocale(), is(nullValue()));

        assertThat(((LocaleContext) englishCtx).isClosed(), is(true));
        assertThat(((LocaleContext) dutchCtx).isClosed(), is(true));
        assertThat(englishCtx.getValue(), equalTo(ENGLISH));
        assertThat(dutchCtx.getValue(), equalTo(DUTCH));
    }

    @Test
    public void testToString() {
        assertThat(manager, hasToString(containsString("LocaleContextManager")));
    }
}
