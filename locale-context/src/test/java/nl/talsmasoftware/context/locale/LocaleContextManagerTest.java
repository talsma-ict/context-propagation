/*
 * Copyright 2016-2018 Talsma ICT
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
import nl.talsmasoftware.context.executors.ContextAwareExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Sjoerd Talsma
 */
public class LocaleContextManagerTest {
    private static final Locale DUTCH = new Locale("nl", "NL");
    private static final Locale ENGLISH = Locale.UK;
    private static final Locale GERMAN = Locale.GERMANY;

    private static LocaleContextManager manager = new LocaleContextManager();
    private ExecutorService threadpool;

    @Before
    public void init() {
        threadpool = new ContextAwareExecutorService(Executors.newCachedThreadPool());
    }

    @After
    public void cleanup() {
        threadpool.shutdown();
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

}
