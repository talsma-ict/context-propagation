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

import io.opentracing.ActiveSpan;
import io.opentracing.NoopTracerFactory;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
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
 * Unit-test for the {@link ActiveSpanManager}.
 *
 * @author Sjoerd Talsma
 */
public class ActiveSpanManagerTest {
    static final ThreadLocalActiveSpanSource TL_ACTIVE_SPAN_SOURCE = new ThreadLocalActiveSpanSource();

    MockTracer mockTracer;
    ExecutorService threadpool;

    @Before
    public void registerMockGlobalTracer() {
        assertThat("Pre-existing GlobalTracer", GlobalTracer.isRegistered(), is(false));
        GlobalTracer.register(mockTracer = new MockTracer(TL_ACTIVE_SPAN_SOURCE));
        threadpool = new ContextAwareExecutorService(Executors.newCachedThreadPool());
    }

    @After
    public void shutdownThreadpool() {
        threadpool.shutdown();
    }

    @After
    public void resetGlobalTracer() throws NoSuchFieldException, IllegalAccessException {
        Field tracer = GlobalTracer.class.getDeclaredField("tracer");
        tracer.setAccessible(true);
        tracer.set(null, NoopTracerFactory.create());
        tracer.setAccessible(false);
    }

    private static final Callable<String> GET_BAGGAGE_ITEM = new Callable<String>() {
        @Override
        public String call() {
            ActiveSpan activeSpan = GlobalTracer.get().activeSpan();
            return activeSpan == null ? "no-active-span" : activeSpan.getBaggageItem("baggage-item");
        }
    };

    @Test
    public void testSingleSnapshotInCurrentThread() throws Exception {
        ActiveSpan outerSpan = mockTracer.buildSpan("first-op").startActive();
        outerSpan.setBaggageItem("baggage-item", "in-outer-span");

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
        ActiveSpan outerSpan = mockTracer.buildSpan("first-op").startActive();
        outerSpan.setBaggageItem("baggage-item", "in-outer-span");

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

}
