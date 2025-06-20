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
package nl.talsmasoftware.context.api;

import nl.talsmasoftware.context.dummy.DummyContext;
import nl.talsmasoftware.context.dummy.DummyContextManager;
import nl.talsmasoftware.context.dummy.DummyContextTimer;
import nl.talsmasoftware.context.dummy.ThrowingContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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
        try (ExecutorService threadPool = Executors.newFixedThreadPool(threadCount)) {
            List<Future<ContextSnapshot>> snapshots = new ArrayList<>(threadCount);
            for (int i = 0; i < threadCount; i++) {
                snapshots.add(threadPool.submit(ContextSnapshot::capture));
            }

            for (int i = 0; i < threadCount; i++) {
                assertThat(snapshots.get(i).get(), is(notNullValue()));
            }
        }
    }

    @Test
    void testCreateSnapshot_ExceptionHandling() {
        ThrowingContextManager.onGet = new IllegalStateException("No active context!");
        Context ctx = new DummyContext("blah");
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
        Context ctx1 = new DummyContext("foo");
        Context ctx2 = mgr.activate("bar");

        ContextSnapshot snapshot = assertDoesNotThrow(ContextSnapshot::capture);
        ThrowingContextManager.onActivate = reactivationException;

        assertThat(DummyContext.currentValue(), is("foo"));
        assertThat(mgr.getActiveContextValue(), is("bar"));
        ctx1.close();
        ctx2.close();

        assertThat(DummyContext.currentValue(), is(nullValue()));
        assertThat(mgr.getActiveContextValue(), is(nullValue()));
        assertThatThrownBy(snapshot::reactivate).isInstanceOf(RuntimeException.class);

        // foo + bar mustn't be set after exception!
        assertThat(DummyContext.currentValue(), is(nullValue()));
        assertThat(mgr.getActiveContextValue(), is(nullValue()));
    }

    @Test
    void testConcurrentSnapshots_fixedClassLoader() throws ExecutionException, InterruptedException {
        ContextManager.useClassLoader(Thread.currentThread().getContextClassLoader());
        int threadCount = 25;
        try (ExecutorService threadPool = Executors.newFixedThreadPool(threadCount)) {
            final List<Future<ContextSnapshot>> snapshots = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                snapshots.add(threadPool.submit(ContextSnapshot::capture));
            }

            for (Future<ContextSnapshot> future : snapshots) {
                assertThat(future.get(), is(notNullValue()));
            }
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

    @Test
    void testReactivationToString() {
        try (ContextSnapshot.Reactivation reactivation = ContextSnapshot.capture().reactivate()) {
            assertThat(reactivation, hasToString(containsString("ContextSnapshot.Reactivation{")));
        }
    }

    @Test
    void capture_exceptionHandling() {
        try (MockedStatic<ServiceCache> ignored = mockStatic(ServiceCache.class)) {
            when(ServiceCache.cached(ContextManager.class)).thenThrow(new IllegalStateException("Service cache error!"));
            assertThatThrownBy(ContextSnapshot::capture)
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("Service cache error!");
        }
    }

    @Test
    void wrapRunnable() {
        dummyManager.activate("Value 1");
        ContextSnapshot snapshot = ContextSnapshot.capture();
        dummyManager.activate("Value 2");

        assertThat(dummyManager.getActiveContextValue(), is("Value 2"));
        snapshot.wrap(() -> assertThat(dummyManager.getActiveContextValue(), is("Value 1"))).run();
        assertThat(dummyManager.getActiveContextValue(), is("Value 2"));
    }

    @Test
    void wrapCallable() throws Exception {
        dummyManager.activate("Value 1");
        ContextSnapshot snapshot = ContextSnapshot.capture();
        dummyManager.activate("Value 2");

        assertThat(dummyManager.getActiveContextValue(), is("Value 2"));
        assertThat(snapshot.wrap(dummyManager::getActiveContextValue).call(), is("Value 1"));
        assertThat(dummyManager.getActiveContextValue(), is("Value 2"));
    }
}
