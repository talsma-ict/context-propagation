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
package nl.talsmasoftware.context.managers.servletrequest;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.core.concurrent.ContextAwareExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletRequest;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ServletRequestContextManagerTest {

    ExecutorService threadpool;

    @BeforeEach
    void init() {
        ServletRequestContextManager.provider().clear();
        threadpool = ContextAwareExecutorService.wrap(Executors.newCachedThreadPool());
    }

    @AfterEach
    void cleanup() {
        threadpool.shutdown();
        ServletRequestContextManager.provider().clear();
    }

    @Test
    void testContextManagerToStringValue() {
        assertThat(ServletRequestContextManager.provider()).hasToString(ServletRequestContextManager.class.getSimpleName());
    }

    @Test
    void testGetActiveContext() {
        assertThat(ServletRequestContextManager.provider().getActiveContextValue()).isNull();

        final ServletRequest request = mock(ServletRequest.class);
        Context ctx = ServletRequestContextManager.provider().activate(request);

        assertThat(ServletRequestContextManager.provider().getActiveContextValue()).isSameAs(request);
        ctx.close();
        assertThat(ServletRequestContextManager.provider().getActiveContextValue()).isNull();
    }

    @Test
    void testCurrentServletRequest() {
        assertThat(ServletRequestContextManager.currentServletRequest()).isNull();

        final ServletRequest request = mock(ServletRequest.class);
        Context ctx = ServletRequestContextManager.provider().activate(request);

        assertThat(ServletRequestContextManager.currentServletRequest()).isSameAs(request);
        ctx.close();
        assertThat(ServletRequestContextManager.currentServletRequest()).isNull();
    }

    @Test
    void testClearableImplementation() {
        final ServletRequest request = mock(ServletRequest.class);
        ServletRequestContextManager.provider().activate(request);
        assertThat(ServletRequestContextManager.provider().getActiveContextValue()).isSameAs(request);

        ContextManager.clearAll();
        assertThat(ServletRequestContextManager.provider().getActiveContextValue()).isNull();
    }

    @Test
    void testPropagationInOtherThreads() throws ExecutionException, InterruptedException {
        ServletRequestContextManager manager = ServletRequestContextManager.provider();
        ServletRequest request1 = mock(ServletRequest.class);
        ServletRequest request2 = mock(ServletRequest.class);

        try (Context ctx1 = manager.activate(request1)) {
            assertThat(manager.getActiveContextValue()).as("Current context").isNotNull();
            assertThat(manager.getActiveContextValue()).as("Current context value").isEqualTo(request1);

            final CountDownLatch blocker = new CountDownLatch(1);
            Future<ServletRequest> slowCall;
            try (Context ctx2 = manager.activate(request2)) {
                slowCall = threadpool.submit(new Callable<ServletRequest>() {
                    public ServletRequest call() throws InterruptedException {
                        blocker.await(5, TimeUnit.SECONDS);
                        return ServletRequestContextManager.currentServletRequest();
                    }
                });

            }
            assertThat(ServletRequestContextManager.currentServletRequest()).as("Restored context in parent").isEqualTo(request1);
            assertThat(slowCall.isDone()).isFalse();
            blocker.countDown();
            assertThat(slowCall.get()).isEqualTo(request2);
        }

        assertThat(manager.getActiveContextValue()).isNull();
    }

    @Test
    void testServletRequestContextToString() {
        ServletRequestContextManager manager = ServletRequestContextManager.provider();
        assertThat(manager.getActiveContextValue()).isNull();

        ServletRequest request = mock(ServletRequest.class);
        Context servletRequestContext = manager.activate(request);
        assertThat(servletRequestContext).hasToString("ServletRequestContext{value=" + request + "}");

        servletRequestContext.close();
        assertThat(servletRequestContext).hasToString("ServletRequestContext{closed}");
    }
}
