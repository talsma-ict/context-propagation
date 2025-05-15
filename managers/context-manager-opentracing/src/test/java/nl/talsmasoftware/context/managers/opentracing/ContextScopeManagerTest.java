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
package nl.talsmasoftware.context.managers.opentracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.GlobalTracerTestUtil;
import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.core.concurrent.ContextAwareExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;

class ContextScopeManagerTest {
    MockTracer mockTracer;
    ContextScopeManager scopeManager;
    ExecutorService threadpool;

    @BeforeEach
    void registerMockGlobalTracer() {
        GlobalTracerTestUtil.resetGlobalTracer();
        assertThat("Pre-existing GlobalTracer", GlobalTracer.isRegistered(), is(false));
        scopeManager = ContextScopeManager.provider();
        GlobalTracer.registerIfAbsent(mockTracer = new MockTracer(scopeManager));
        threadpool = new ContextAwareExecutorService(Executors.newCachedThreadPool());
    }

    @AfterEach
    void cleanup() {
        threadpool.shutdown();
        ContextManager.clearAll();
        GlobalTracerTestUtil.resetGlobalTracer();
    }

    @Test
    @Disabled("TODO replace observer by MockTracer .finishedSpans inspection!")
    void testConcurrency() throws InterruptedException {
        List<Thread> threads = new ArrayList<Thread>();
        final Span parentSpan = GlobalTracer.get().buildSpan("parent").start();
        final Scope parent = GlobalTracer.get().activateSpan(parentSpan);
        final CountDownLatch latch1 = new CountDownLatch(10), latch2 = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            final int nr = i;
            threads.add(new Thread() {
                @Override
                public void run() {
                    Span innerSpan = GlobalTracer.get().buildSpan("inner" + nr).asChildOf(parentSpan).start();
                    Scope inner = GlobalTracer.get().activateSpan(innerSpan);
                    waitFor(latch1);
                    innerSpan.finish();
                    inner.close();
                    waitFor(latch2);
                }
            });
        }

        assertThat(GlobalTracer.get().activeSpan(), equalTo(parentSpan));
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        parentSpan.finish();
        parent.close();
        assertThat(GlobalTracer.get().activeSpan(), is(nullValue()));
    }

    @Test
    void testInitializeNewContext() {
        Span span = GlobalTracer.get().buildSpan("span").start();
        Context context = scopeManager.initializeNewContext(span);
        assertThat(scopeManager.getActiveContextValue(), is(span));
        assertThat(scopeManager.activeSpan(), is(span));
        assertThat(GlobalTracer.get().activeSpan(), is(span));
        context.close();
        span.finish();
    }

    @Test
    void testSimpleToStringWhenLogged() {
        assertThat(scopeManager, hasToString(scopeManager.getClass().getSimpleName()));
    }

    @Test
    void testPredictableOutOfOrderClosing() {
        Span firstSpan = GlobalTracer.get().buildSpan("first").start();
        Scope first = GlobalTracer.get().activateSpan(firstSpan);
        Span secondSpan = GlobalTracer.get().buildSpan("second").start();
        Scope second = GlobalTracer.get().activateSpan(secondSpan);
        first.close();
        assertThat(GlobalTracer.get().activeSpan(), is(secondSpan));
        second.close();
        assertThat(GlobalTracer.get().activeSpan(), is(nullValue())); // first was already closed
    }

    static void waitFor(CountDownLatch latch) {
        try {
            latch.countDown();
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            fail("Interrupted during test..");
        }
    }
}
