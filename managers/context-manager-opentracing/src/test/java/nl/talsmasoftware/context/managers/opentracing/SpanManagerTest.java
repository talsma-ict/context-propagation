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
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.GlobalTracerTestUtil;
import io.opentracing.util.ThreadLocalScopeManager;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.core.concurrent.ContextAwareExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Unit-test for the {@link SpanManager}.
 *
 * @author Sjoerd Talsma
 */
public class SpanManagerTest {
    static final ScopeManager SCOPE_MANAGER = new ThreadLocalScopeManager();

    MockTracer mockTracer;
    ExecutorService threadpool;

    @BeforeEach
    public void registerMockGlobalTracer() {
        GlobalTracerTestUtil.resetGlobalTracer();
        assertThat("Pre-existing GlobalTracer", GlobalTracer.isRegistered(), is(false));
        GlobalTracer.register(mockTracer = new MockTracer(SCOPE_MANAGER));
        threadpool = new ContextAwareExecutorService(Executors.newCachedThreadPool());
    }

    @AfterEach
    public void cleanup() {
        threadpool.shutdown();
        GlobalTracerTestUtil.resetGlobalTracer();
    }

    @BeforeEach
    @AfterEach
    public void removeActiveSope() throws NoSuchFieldException, IllegalAccessException {
        Field tlsScope = ThreadLocalScopeManager.class.getDeclaredField("tlsScope");
        tlsScope.setAccessible(true);
        ((ThreadLocal) tlsScope.get(SCOPE_MANAGER)).remove();
    }

    private static final Callable<String> GET_BAGGAGE_ITEM = new Callable<String>() {
        @Override
        public String call() {
            Span activeSpan = GlobalTracer.get().scopeManager().activeSpan();
            return activeSpan == null ? "no-active-span" : activeSpan.getBaggageItem("baggage-item");
        }
    };

    @Test
    @Disabled("TODO Investigate the problem with this test and fix it before releasing!")
    public void testSingleSnapshotInBackgroundThread() throws Exception {
        Span outerSpan = mockTracer.buildSpan("first-op").start();
        Scope outerScope = mockTracer.scopeManager().activate(outerSpan);
        outerSpan.setBaggageItem("baggage-item", "in-outer-span");

        // sanity-check: outerSpan should be the active span..
        assertThat("sanity-check", GET_BAGGAGE_ITEM.call(), equalTo("in-outer-span"));

        // The active span reference should propagate to the background thread and therefore return the baggage.
        Future<String> backgroundBaggage = threadpool.submit(GET_BAGGAGE_ITEM);
        assertThat("background baggage", backgroundBaggage.get(), equalTo("in-outer-span"));
        assertThat("span finished?", mockTracer.finishedSpans(), not(hasItem((MockSpan) outerSpan)));

        outerScope.close();
        assertThat("span finished?", mockTracer.finishedSpans(), not(hasItem((MockSpan) outerSpan)));
        assertThat("baggage of active span", GET_BAGGAGE_ITEM.call(), equalTo("no-active-span"));
    }

    @Test
    @Disabled("TODO Investigate the problem with this test and fix it before releasing!")
    public void testFinishSpanFromBlockingBackgroundThread() throws Exception {
        Span outerSpan = mockTracer.buildSpan("first-op").start();
        Scope outerScope = mockTracer.scopeManager().activate(outerSpan);
        outerSpan.setBaggageItem("baggage-item", "in-outer-span");

        // sanity-check: outerSpan should be the active span.
        assertThat("sanity-check", GET_BAGGAGE_ITEM.call(), equalTo("in-outer-span"));

        // Start a blocking background thread.
        final Lock lock = new ReentrantLock();
        lock.lock();
        Future<String> blockingBackgroundBaggage = threadpool.submit(new Callable<String>() {
            public String call() throws Exception {
                if (!lock.tryLock(5, TimeUnit.MINUTES)) throw new IllegalStateException("Couldn't obtain lock!");
                try {
                    return GET_BAGGAGE_ITEM.call();
                } finally {
                    GlobalTracer.get().scopeManager().activeSpan().finish();
                    lock.unlock();
                }
            }
        });

        assertThat("background thread blocked", blockingBackgroundBaggage.isDone(), is(false));
        assertThat("span finished?", mockTracer.finishedSpans(), not(hasItem((MockSpan) outerSpan)));

        // Close outer span (it shouldn't finish until the background thread finishes).
        outerScope.close();

        assertThat("span finished?", mockTracer.finishedSpans(), not(hasItem((MockSpan) outerSpan)));

        // Let the blocking thread finish and check that the span gets closed.
        lock.unlock();
        assertThat("unblocked baggage", blockingBackgroundBaggage.get(), equalTo("in-outer-span"));
        assertThat("span finished?", mockTracer.finishedSpans(), hasItem((MockSpan) outerSpan));
    }

    @Test
    public void testLonglivedChildSpanFromBackgroundThread() throws Exception {
        Span parentSpan = mockTracer.buildSpan("first-op").start();
        Scope parent = mockTracer.activateSpan(parentSpan);
        parentSpan.setBaggageItem("baggage-item", "in-outer-span");

        // sanity-check: outerSpan should be the active span..
        assertThat("sanity-check", GET_BAGGAGE_ITEM.call(), equalTo("in-outer-span"));

        // Start a blocking background thread.
        final Lock lock = new ReentrantLock();
        lock.lock();
        final AtomicReference<Span> childSpanRef = new AtomicReference<Span>();
        Future<String> blockingBackgroundBaggage = threadpool.submit(new Callable<String>() {
            public String call() throws Exception {
                Span childSpan = GlobalTracer.get().buildSpan("child-op").start();
                Scope child = GlobalTracer.get().activateSpan(childSpan);
                childSpanRef.set(childSpan);
                try {
                    childSpan.setBaggageItem("baggage-item", "in-child-span");
                    assertThat("active-span", SCOPE_MANAGER.activeSpan(), is(sameInstance(childSpan)));
                    if (!lock.tryLock(5, TimeUnit.MINUTES)) throw new IllegalStateException("Couldn't obtain lock!");
                    assertThat("active-span", SCOPE_MANAGER.activeSpan(), is(sameInstance(childSpan)));
                    return GET_BAGGAGE_ITEM.call();
                } finally {
                    child.close();
                    childSpan.finish();
                    lock.unlock();
                }
            }
        });

        assertThat("background thread blocked", blockingBackgroundBaggage.isDone(), is(false));
        assertThat("parent span finished?", mockTracer.finishedSpans(), not(hasItem((MockSpan) parentSpan)));
        assertThat("child span finished?", mockTracer.finishedSpans(), not(hasItem((MockSpan) childSpanRef.get())));

        // Close outer span (child shouldn't finish until the background thread finishes, the outer span will).
        assertThat(GET_BAGGAGE_ITEM.call(), is("in-outer-span"));
        assertThat("active-span", SCOPE_MANAGER.activeSpan(), is(sameInstance(parentSpan)));
        parent.close();
        parentSpan.finish();
        assertThat("active-span", SCOPE_MANAGER.activeSpan(), is(nullValue()));
        assertThat(GET_BAGGAGE_ITEM.call(), is("no-active-span"));

        assertThat("parent span finished?", mockTracer.finishedSpans(), hasItem((MockSpan) parentSpan));
        assertThat("child span finished?", mockTracer.finishedSpans(), not(hasItem((MockSpan) childSpanRef.get())));

        // So the outer span actually closed sooner than its child, because that got propagated to the (blocked)
        // background thread.

        // Let the blocking thread finish and check that the span gets closed.
        lock.unlock();
        assertThat("unblocked baggage", blockingBackgroundBaggage.get(), equalTo("in-child-span"));
        assertThat("active-span", GlobalTracer.get().scopeManager().activeSpan(), is(nullValue()));
        assertThat("child span finished?", mockTracer.finishedSpans(), hasItem((MockSpan) childSpanRef.get()));
    }

    @Test
    public void testClearingAllContexts() {
        Span span = mockTracer.buildSpan("test-span").start();
        Scope scope = mockTracer.scopeManager().activate(span);
        assertThat(SpanManager.provider().getActiveContextValue(), is(sameInstance(span)));

        ContextManager.clearAll();
        // TODO Test after this is merged: https://github.com/opentracing/opentracing-java/pull/313
        // assertThat(SpanManager.provider().getActiveContextValue(), is(nullValue()));
    }

    @Test
    public void testSpanManagerToString() {
        assertThat(SpanManager.provider(), hasToString("SpanManager"));
    }

    @Test
    public void testSpanContextToString() {
        Span span = mockTracer.buildSpan("test-span").start();
        assertThat(SpanManager.provider().getActiveContextValue(), nullValue());

        Scope scope = mockTracer.scopeManager().activate(span);
        assertThat(SpanManager.provider().getActiveContextValue(), is(span));

        scope.close();
        assertThat(SpanManager.provider().getActiveContextValue(), nullValue());
    }
}
