/*
 * Copyright 2016-2019 Talsma ICT
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
package nl.talsmasoftware.context.opentracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.GlobalTracerTestUtil;
import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.executors.ContextAwareExecutorService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static nl.talsmasoftware.context.opentracing.ContextScopeManagerObserver.EventMatcher.activated;
import static nl.talsmasoftware.context.opentracing.ContextScopeManagerObserver.EventMatcher.deactivated;
import static nl.talsmasoftware.context.opentracing.MockSpanMatcher.withOperationName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

public class ContextScopeManagerTest {
    MockTracer mockTracer;
    ContextScopeManager scopeManager;
    ExecutorService threadpool;

    @Before
    public void registerMockGlobalTracer() {
        GlobalTracerTestUtil.resetGlobalTracer();
        assertThat("Pre-existing GlobalTracer", GlobalTracer.isRegistered(), is(false));
        scopeManager = new ContextScopeManager();
        GlobalTracer.register(mockTracer = new MockTracer(scopeManager));
        threadpool = new ContextAwareExecutorService(Executors.newCachedThreadPool());
    }

    @After
    public void cleanup() {
        threadpool.shutdown();
        ContextManagers.clearActiveContexts();
        GlobalTracerTestUtil.resetGlobalTracer();
        ContextScopeManagerObserver.observed.clear();
    }

    @Test
    public void testObservedSpans() {
        assertThat(ContextScopeManagerObserver.observed, is(empty()));
        Scope parent = GlobalTracer.get().buildSpan("parent").startActive(true);
        Scope inner = GlobalTracer.get().buildSpan("inner").startActive(true);
        inner.close();
        parent.close();

        assertThat(ContextScopeManagerObserver.observed, contains(
                activated(withOperationName("parent")),
                activated(withOperationName("inner")),
                deactivated(withOperationName("inner")),
                deactivated(withOperationName("parent"))
        ));
    }

    @Test
    public void testConcurrency() throws InterruptedException {
        List<Thread> threads = new ArrayList<Thread>();
        final Scope parent = GlobalTracer.get().buildSpan("parent").startActive(true);
        final CountDownLatch latch1 = new CountDownLatch(10), latch2 = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            final int nr = i;
            threads.add(new Thread() {
                @Override
                public void run() {
                    Scope inner = GlobalTracer.get().buildSpan("inner" + nr).asChildOf(parent.span()).startActive(true);
                    waitFor(latch1);
                    inner.close();
                    waitFor(latch2);
                    parent.close();
                }
            });
        }
        assertThat(ContextScopeManagerObserver.observed, contains(
                activated(withOperationName("parent"))
        ));

        assertThat(GlobalTracer.get().activeSpan(), equalTo(parent.span()));
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        assertThat(GlobalTracer.get().activeSpan(), is(nullValue()));
        assertThat(ContextScopeManagerObserver.observed, contains(
                activated(withOperationName("parent")),
                activated(withOperationName(startsWith("inner"))),
                activated(withOperationName(startsWith("inner"))),
                activated(withOperationName(startsWith("inner"))),
                activated(withOperationName(startsWith("inner"))),
                activated(withOperationName(startsWith("inner"))),
                activated(withOperationName(startsWith("inner"))),
                activated(withOperationName(startsWith("inner"))),
                activated(withOperationName(startsWith("inner"))),
                activated(withOperationName(startsWith("inner"))),
                activated(withOperationName(startsWith("inner"))),
                deactivated(withOperationName(startsWith("inner"))),
                deactivated(withOperationName(startsWith("inner"))),
                deactivated(withOperationName(startsWith("inner"))),
                deactivated(withOperationName(startsWith("inner"))),
                deactivated(withOperationName(startsWith("inner"))),
                deactivated(withOperationName(startsWith("inner"))),
                deactivated(withOperationName(startsWith("inner"))),
                deactivated(withOperationName(startsWith("inner"))),
                deactivated(withOperationName(startsWith("inner"))),
                deactivated(withOperationName(startsWith("inner"))),
                deactivated(withOperationName("parent"))
        ));
    }

    @Test
    public void testInitializeNewContext() {
        Span span = GlobalTracer.get().buildSpan("span").start();
        Context<Span> context = scopeManager.initializeNewContext(span);
        assertThat(scopeManager.getActiveContext().getValue(), is(span));
        assertThat(scopeManager.active().span(), is(span));
        assertThat(GlobalTracer.get().activeSpan(), is(span));
        context.close();
        span.finish();
    }

    @Test
    public void testSimpleToStringWhenLogged() {
        assertThat(scopeManager, hasToString(scopeManager.getClass().getSimpleName()));
    }

    @Test
    public void testPredictableOutOfOrderClosing() {
        Scope first = GlobalTracer.get().buildSpan("first").startActive(true);
        Scope second = GlobalTracer.get().buildSpan("second").startActive(true);
        first.close();
        assertThat(GlobalTracer.get().activeSpan(), is(second.span()));
        second.close();
        assertThat(GlobalTracer.get().activeSpan(), is(nullValue())); // first was already closed
    }

    private static void waitFor(CountDownLatch latch) {
        try {
            latch.countDown();
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            Assert.fail("Interrupted during test..");
        }
    }
}
