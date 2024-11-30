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
package nl.talsmasoftware.context.api;

import nl.talsmasoftware.context.dummy.DummyContext;
import nl.talsmasoftware.context.dummy.DummyContextManager;
import nl.talsmasoftware.context.dummy.DummyContextTimer;
import nl.talsmasoftware.context.dummy.ThrowingContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Sjoerd Talsma
 */
class ContextSnapshotTest {
    DummyContextManager dummyManager = new DummyContextManager();

    @BeforeEach
    @AfterEach
    void resetContexts() {
        ContextManager.clearAll();
        DummyContextTimer.clear();
    }

    @BeforeEach
    @AfterEach
    void resetContextClassLoader() {
        ContextManager.useClassLoader(null);
    }

    @Test
    void testSnapshot_inSameThread() {
        dummyManager.clear();
        assertThat(DummyContext.currentValue(), is(nullValue()));

        DummyContext ctx1 = new DummyContext("initial value");
        assertThat(DummyContext.currentValue(), is("initial value"));

        DummyContext ctx2 = new DummyContext("second value");
        assertThat(DummyContext.currentValue(), is("second value"));

        ContextSnapshot snapshot = ContextSnapshot.capture();
        assertThat(DummyContext.currentValue(), is("second value")); // No context change because of snapshot.

        DummyContext ctx3 = new DummyContext("third value");
        assertThat(DummyContext.currentValue(), is("third value"));

        // Reactivate snapshot: ctx1 -> ctx2 -> ctx3 -> ctx2'
        ContextSnapshot.Reactivation reactivation = snapshot.reactivate();
        assertThat(DummyContext.currentValue(), is("second value"));

        reactivation.close();
        assertThat(DummyContext.currentValue(), is("third value")); // back to ctx3, NOT ctx1 !!

        // out-of-order closing!
        ctx2.close();
        assertThat(DummyContext.currentValue(), is("third value")); // back to ctx3, NOT ctx1 !!

        ctx3.close();
        assertThat(DummyContext.currentValue(), is("initial value")); // back to ctx1 because ctx2 is closed

        assertThat(ctx1.isClosed(), is(false));
        assertThat(ctx2.isClosed(), is(true));
        assertThat(ctx3.isClosed(), is(true));
        ctx1.close();
    }

    @Test
    void testConcurrentSnapshots() throws ExecutionException, InterruptedException {
        int threadCount = 25;
        ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<ContextSnapshot>> snapshots = new ArrayList<>(threadCount);
            for (int i = 0; i < threadCount; i++) {
                snapshots.add(threadPool.submit(ContextSnapshot::capture));
            }

            for (int i = 0; i < threadCount; i++) {
                assertThat(snapshots.get(i).get(), is(notNullValue()));
            }
        } finally {
            threadPool.shutdown();
        }
    }

    @Test
    void testCreateSnapshot_ExceptionHandling() {
        ThrowingContextManager.onGet = new IllegalStateException("No active context!");
        Context<String> ctx = new DummyContext("blah");
        ContextSnapshot snapshot = ContextSnapshot.capture();
        ctx.close();

        assertThat(DummyContext.currentValue(), is(nullValue()));
        ContextSnapshot.Reactivation reactivation = snapshot.reactivate();
        assertThat(DummyContext.currentValue(), is("blah"));
        reactivation.close();
        assertThat(DummyContext.currentValue(), is(nullValue()));
    }

    @Test
    void testReactivateSnapshot_ExceptionHandling() {
        final RuntimeException reactivationException = new IllegalStateException("Cannot create new context!");
        ThrowingContextManager mgr = new ThrowingContextManager();
        Context<String> ctx1 = new DummyContext("foo");
        Context<String> ctx2 = mgr.initializeNewContext("bar");
        ContextSnapshot snapshot = ContextSnapshot.capture();
        ThrowingContextManager.onInitialize = reactivationException;

        assertThat(DummyContext.currentValue(), is("foo"));
        assertThat(mgr.getActiveContextValue(), is("bar"));
        ctx1.close();
        ctx2.close();

        assertThat(DummyContext.currentValue(), is(nullValue()));
        assertThat(mgr.getActiveContextValue(), is(nullValue()));
        RuntimeException expected = assertThrows(RuntimeException.class, snapshot::reactivate);

        // foo + bar mustn't be set after exception!
        assertThat(DummyContext.currentValue(), is(nullValue()));
        assertThat(mgr.getActiveContextValue(), is(nullValue()));
    }

    @Test
    void testConcurrentSnapshots_fixedClassLoader() throws ExecutionException, InterruptedException {
        ContextManager.useClassLoader(Thread.currentThread().getContextClassLoader());
        int threadcount = 25;
        ExecutorService threadpool = Executors.newFixedThreadPool(threadcount);
        try {
            Future<ContextSnapshot>[] snapshots = new Future[threadcount];
            for (int i = 0; i < threadcount; i++) {
                snapshots[i] = threadpool.submit(ContextSnapshot::capture);
            }

            for (int i = 0; i < threadcount; i++) {
                assertThat("Future " + i, snapshots[i], notNullValue());
                assertThat("Snapshot " + i, snapshots[i].get(), notNullValue());
            }
        } finally {
            threadpool.shutdown();
        }
    }

    @Test
    void toString_isForSnapshot_notSnapshotImpl() {
        assertThat(ContextSnapshot.capture(), hasToString(containsString("ContextSnapshot{")));
    }

    @Test
    void testTimingDelegation() {
        DummyContextTimer.clear();
        assertThat(DummyContextTimer.getLastTimedMillis(ContextSnapshot.class, "capture"), nullValue());
        assertThat(DummyContextTimer.getLastTimedMillis(ContextSnapshot.class, "reactivate"), nullValue());

        ContextSnapshot.capture().reactivate().close();
        assertThat(DummyContextTimer.getLastTimedMillis(ContextSnapshot.class, "capture"), notNullValue());
        assertThat(DummyContextTimer.getLastTimedMillis(ContextSnapshot.class, "reactivate"), notNullValue());
    }

}
