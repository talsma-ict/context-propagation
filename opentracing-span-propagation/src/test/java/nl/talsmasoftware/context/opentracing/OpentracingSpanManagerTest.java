/*
 * Copyright 2016-2017 Talsma ICT
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
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.ThreadLocalScopeManager;
import nl.talsmasoftware.context.executors.ContextAwareExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit-test for the {@link OpentracingSpanManager}.
 *
 * @author Sjoerd Talsma
 */
public class OpentracingSpanManagerTest {
    static final ScopeManager SCOPE_MANAGER = new ThreadLocalScopeManager();

    MockTracer mockTracer;
    ExecutorService threadpool;

    @Before
    public void registerMockGlobalTracer() {
        resetGlobalTracer();
        assertThat("Pre-existing GlobalTracer", GlobalTracer.isRegistered(), is(false));
        GlobalTracer.register(mockTracer = new MockTracer(SCOPE_MANAGER));
        threadpool = new ContextAwareExecutorService(Executors.newCachedThreadPool());
    }

    @After
    public void shutdownThreadpool() {
        threadpool.shutdown();
    }

    @After
    public void resetGlobalTracer() {
        try {
            Field tracer = GlobalTracer.class.getDeclaredField("tracer");
            tracer.setAccessible(true);
            tracer.set(null, NoopTracerFactory.create());
            tracer.setAccessible(false);
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't reset the Global Tracer: " + e.getMessage(), e);
        }
    }

    private static final Callable<String> GET_BAGGAGE_ITEM = new Callable<String>() {
        @Override
        public String call() {
            Scope activeScope = GlobalTracer.get().scopeManager().active();
            Span activeSpan = activeScope == null ? null : activeScope.span();
            return activeSpan == null ? "no-active-span" : activeSpan.getBaggageItem("baggage-item");
        }
    };

    @Test
    public void testSingleSnapshotInBackgroundThread() throws Exception {
        Scope outerSpan = mockTracer.buildSpan("first-op").startActive();
        outerSpan.span().setBaggageItem("baggage-item", "in-outer-span");

        // sanity-check: outerSpan should be the active span..
        assertThat("sanity-check", GET_BAGGAGE_ITEM.call(), equalTo("in-outer-span"));

        // The active span reference should propagate to the background thread and therefore return the baggage.
        Future<String> backgroundBaggage = threadpool.submit(GET_BAGGAGE_ITEM);
        assertThat("background baggage", backgroundBaggage.get(), equalTo("in-outer-span"));
        assertThat("span finished?", mockTracer.finishedSpans(), is(emptyCollectionOf(MockSpan.class)));

        outerSpan.close();
        assertThat("span finished?", mockTracer.finishedSpans(), hasSize(1));
        assertThat("baggage of active span", GET_BAGGAGE_ITEM.call(), equalTo("no-active-span"));
    }

    @Test
    public void testFinishSpanFromBlockingBackgroundThread() throws Exception {
        Scope outerSpan = mockTracer.buildSpan("first-op").startActive(false);
        outerSpan.span().setBaggageItem("baggage-item", "in-outer-span");

        // sanity-check: outerSpan should be the active span..
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
                    GlobalTracer.get().scopeManager().active().span().finish();
                    lock.unlock();
                }
            }
        });

        assertThat("background thread blocked", blockingBackgroundBaggage.isDone(), is(false));
        assertThat("span finished?", mockTracer.finishedSpans(), is(emptyCollectionOf(MockSpan.class)));

        // Close outer span (it shouldn't finish until the background thread finishes).
        outerSpan.close();

        assertThat("span finished?", mockTracer.finishedSpans(), is(emptyCollectionOf(MockSpan.class)));

        // Let the blocking thread finish and check that the span gets closed.
        lock.unlock();
        assertThat("unblocked baggage", blockingBackgroundBaggage.get(), equalTo("in-outer-span"));
        assertThat("span finished?", mockTracer.finishedSpans(), hasSize(1));
    }

    @Test
    public void testLonglivedChildSpanFromBackgroundThread() throws Exception {
        Scope parent = mockTracer.buildSpan("first-op").startActive(true);
        parent.span().setBaggageItem("baggage-item", "in-outer-span");

        // sanity-check: outerSpan should be the active span..
        assertThat("sanity-check", GET_BAGGAGE_ITEM.call(), equalTo("in-outer-span"));

//        Scope childSpan = mockTracer.buildSpan("child-op").startActive();
//        childSpan.span().setBaggageItem("baggage-item", "in-child-span");
//
//        // sanity-check: childSpan should be the active span..
//        assertThat("sanity-check", GET_BAGGAGE_ITEM.call(), equalTo("in-child-span"));

        // Start a blocking background thread.
        final Lock lock = new ReentrantLock();
        lock.lock();
        Future<String> blockingBackgroundBaggage = threadpool.submit(new Callable<String>() {
            public String call() throws Exception {
                Scope child = GlobalTracer.get().buildSpan("child-op").startActive(true);
                try {
                    child.span().setBaggageItem("baggage-item", "in-child-span");
                    assertThat("active-span", GlobalTracer.get().scopeManager().active().span(), is(sameInstance(child.span())));
                    if (!lock.tryLock(5, TimeUnit.MINUTES)) throw new IllegalStateException("Couldn't obtain lock!");
                    assertThat("active-span", GlobalTracer.get().scopeManager().active().span(), is(sameInstance(child.span())));
                    return GET_BAGGAGE_ITEM.call();
                } finally {
                    child.close();
                    lock.unlock();
                }
            }
        });

        assertThat("background thread blocked", blockingBackgroundBaggage.isDone(), is(false));
        assertThat("no span finished?", mockTracer.finishedSpans(), is(emptyCollectionOf(MockSpan.class)));

        // Close outer span (child shouldn't finish until the background thread finishes, the outer span will).
        assertThat(GET_BAGGAGE_ITEM.call(), is("in-outer-span"));
        assertThat("active-span", GlobalTracer.get().scopeManager().active().span(), is(sameInstance(parent.span())));
        parent.close();
        assertThat(GET_BAGGAGE_ITEM.call(), is("no-active-span"));
        assertThat("active-span", GlobalTracer.get().scopeManager().active(), is(nullValue()));

        assertThat("outer span finished?", mockTracer.finishedSpans(), hasSize(1));
        assertThat(mockTracer.finishedSpans().get(0).getBaggageItem("baggage-item"), is("in-outer-span"));
        assertThat(mockTracer.finishedSpans().get(0).parentId(), is(0L));
        // So the outer span actually closed sooner than its child, because that got propagated to the (blocked)
        // background thread.

        // Let the blocking thread finish and check that the span gets closed.
        lock.unlock();
        assertThat("unblocked baggage", blockingBackgroundBaggage.get(), equalTo("in-child-span"));
        assertThat("active-span", GlobalTracer.get().scopeManager().active(), is(nullValue()));
        assertThat("span finished?", mockTracer.finishedSpans(), hasSize(2));
        assertThat(mockTracer.finishedSpans().get(1).getBaggageItem("baggage-item"), is("in-child-span"));
        assertThat(mockTracer.finishedSpans().get(1).parentId(), is(not(0L)));
    }

}
