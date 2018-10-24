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
package nl.talsmasoftware.context.servletrequest;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.executors.ContextAwareExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletRequest;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;

public class ServletRequestContextManagerTest {

    private ExecutorService threadpool;

    @Before
    public void init() {
        ServletRequestContextManager.clear();
        threadpool = new ContextAwareExecutorService(Executors.newCachedThreadPool());
    }

    @After
    public void cleanup() {
        threadpool.shutdown();
        ServletRequestContextManager.clear();
    }

    @Test
    public void testContextManagerToStringValue() {
        assertThat(new ServletRequestContextManager(), hasToString(ServletRequestContextManager.class.getSimpleName()));
    }

    @Test
    public void testGetActiveContext() {
        assertThat(new ServletRequestContextManager().getActiveContext(), is(nullValue()));

        final ServletRequest request = mock(ServletRequest.class);
        Context<ServletRequest> ctx = new ServletRequestContextManager().initializeNewContext(request);

        assertThat(new ServletRequestContextManager().getActiveContext().getValue(), is(sameInstance(request)));
        ctx.close();
        assertThat(new ServletRequestContextManager().getActiveContext(), is(nullValue()));
    }

    @Test
    public void testCurrentServletRequest() {
        assertThat(ServletRequestContextManager.currentServletRequest(), is(nullValue()));

        final ServletRequest request = mock(ServletRequest.class);
        Context<ServletRequest> ctx = new ServletRequestContextManager().initializeNewContext(request);

        assertThat(ServletRequestContextManager.currentServletRequest(), is(sameInstance(request)));
        ctx.close();
        assertThat(ServletRequestContextManager.currentServletRequest(), is(nullValue()));
    }

    @Test
    public void testClearableImplementation() {
        final ServletRequest request = mock(ServletRequest.class);
        Context<ServletRequest> ctx = new ServletRequestContextManager().initializeNewContext(request);
        assertThat(ctx.getValue(), is(sameInstance(request)));
        assertThat(new ServletRequestContextManager().getActiveContext().getValue(), is(sameInstance(request)));

        ContextManagers.clearActiveContexts();
        assertThat(ctx.getValue(), is(nullValue())); // must have been closed
        assertThat(new ServletRequestContextManager().getActiveContext(), is(nullValue()));
    }

    @Test
    public void testPropagationInOtherThreads() throws ExecutionException, InterruptedException {
        ServletRequestContextManager manager = new ServletRequestContextManager();
        ServletRequest request1 = mock(ServletRequest.class);
        ServletRequest request2 = mock(ServletRequest.class);

        Context<ServletRequest> ctx1 = manager.initializeNewContext(request1);
        try {
            assertThat("Current context", manager.getActiveContext(), is(notNullValue()));
            assertThat("Current context value", manager.getActiveContext().getValue(), is(request1));

            Context<ServletRequest> ctx2 = manager.initializeNewContext(request2);
            final CountDownLatch blocker = new CountDownLatch(1);
            Future<ServletRequest> slowCall;
            try {

                slowCall = threadpool.submit(new Callable<ServletRequest>() {
                    public ServletRequest call() throws InterruptedException {
                        blocker.await(5, TimeUnit.SECONDS);
                        return ServletRequestContextManager.currentServletRequest();
                    }
                });

            } finally {
                ctx2.close();
            }
            assertThat("Closed context in parent", ServletRequestContextManager.currentServletRequest(), is(nullValue()));
            assertThat("Slow thread already done", slowCall.isDone(), is(false));
            blocker.countDown();
            assertThat("Context in slow thread", slowCall.get(), is(request2));

        } finally {
            ctx1.close();
        }
        assertThat("Current closed context", manager.getActiveContext(), is(nullValue()));
    }

}
