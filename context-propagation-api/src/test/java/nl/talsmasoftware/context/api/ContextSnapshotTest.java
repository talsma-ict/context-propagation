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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        assertThat(DummyContext.currentValue()).isNull();

        DummyContext ctx1 = new DummyContext("initial value");
        assertThat(DummyContext.currentValue()).isEqualTo("initial value");

        DummyContext ctx2 = new DummyContext("second value");
        assertThat(DummyContext.currentValue()).isEqualTo("second value");

        ContextSnapshot snapshot = ContextSnapshot.capture();
        assertThat(DummyContext.currentValue()).isEqualTo("second value"); // No context change because of snapshot.

        DummyContext ctx3 = new DummyContext("third value");
        assertThat(DummyContext.currentValue()).isEqualTo("third value");

        // Reactivate snapshot: ctx1 -> ctx2 -> ctx3 -> ctx2'
        ContextSnapshot.Reactivation reactivation = snapshot.reactivate();
        assertThat(DummyContext.currentValue()).isEqualTo("second value");

        reactivation.close();
        assertThat(DummyContext.currentValue()).isEqualTo("third value"); // back to ctx3, NOT ctx1 !!

        // out-of-order closing!
        ctx2.close();
        assertThat(DummyContext.currentValue()).isEqualTo("third value"); // back to ctx3, NOT ctx1 !!

        ctx3.close();
        assertThat(DummyContext.currentValue()).isEqualTo("initial value"); // back to ctx1 because ctx2 is closed

        assertThat(ctx1.isClosed()).isFalse();
        assertThat(ctx2.isClosed()).isTrue();
        assertThat(ctx3.isClosed()).isTrue();
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
                assertThat(snapshots.get(i).get()).isNotNull();
            }
        }
    }

    @Test
    void testCreateSnapshot_ExceptionHandling() {
        ThrowingContextManager.onGet = new IllegalStateException("No active context!");
        Context ctx = new DummyContext("blah");
        ContextSnapshot snapshot = ContextSnapshot.capture();
        ctx.close();

        assertThat(DummyContext.currentValue()).isNull();
        ContextSnapshot.Reactivation reactivation = snapshot.reactivate();
        assertThat(DummyContext.currentValue()).isEqualTo("blah");
        reactivation.close();
        assertThat(DummyContext.currentValue()).isNull();
    }

    @Test
    void testReactivateSnapshot_ExceptionHandling() {
        final RuntimeException reactivationException = new IllegalStateException("Cannot create new context!");
        ThrowingContextManager mgr = new ThrowingContextManager();
        Context ctx1 = new DummyContext("foo");
        Context ctx2 = mgr.activate("bar");

        ContextSnapshot snapshot = assertDoesNotThrow(ContextSnapshot::capture);
        ThrowingContextManager.onActivate = reactivationException;

        assertThat(DummyContext.currentValue()).isEqualTo("foo");
        assertThat(mgr.getActiveContextValue()).isEqualTo("bar");
        ctx1.close();
        ctx2.close();

        assertThat(DummyContext.currentValue()).isNull();
        assertThat(mgr.getActiveContextValue()).isNull();
        assertThatThrownBy(snapshot::reactivate).isInstanceOf(RuntimeException.class);

        // foo + bar mustn't be set after exception!
        assertThat(DummyContext.currentValue()).isNull();
        assertThat(mgr.getActiveContextValue()).isNull();
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
                assertThat(future.get()).isNotNull();
            }
        }
    }

    @Test
    void toString_isForSnapshot_notSnapshotImpl() {
        assertThat(ContextSnapshot.capture().toString()).contains("ContextSnapshot{");
    }

    @Test
    void testTimingDelegation() {
        DummyContextTimer.clear();
        assertThat(DummyContextTimer.getLastTimedMillis(ContextSnapshot.class, "capture")).isNull();
        assertThat(DummyContextTimer.getLastTimedMillis(ContextSnapshot.class, "reactivate")).isNull();

        ContextSnapshot.capture().reactivate().close();
        assertThat(DummyContextTimer.getLastTimedMillis(ContextSnapshot.class, "capture")).isNotNull();
        assertThat(DummyContextTimer.getLastTimedMillis(ContextSnapshot.class, "reactivate")).isNotNull();
    }

    @Test
    void testReactivationToString() {
        try (ContextSnapshot.Reactivation reactivation = ContextSnapshot.capture().reactivate()) {
            assertThat(reactivation.toString()).contains("ContextSnapshot.Reactivation{");
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

        assertThat(dummyManager.getActiveContextValue()).isEqualTo("Value 2");
        snapshot.wrap((Runnable) () -> assertThat(dummyManager.getActiveContextValue()).isEqualTo("Value 1")).run();
        assertThat(dummyManager.getActiveContextValue()).isEqualTo("Value 2");
    }

    @Test
    void wrapCallable() throws Exception {
        dummyManager.activate("Value 1");
        ContextSnapshot snapshot = ContextSnapshot.capture();
        dummyManager.activate("Value 2");

        assertThat(dummyManager.getActiveContextValue()).isEqualTo("Value 2");
        assertThat(snapshot.wrap(dummyManager::getActiveContextValue).call()).isEqualTo("Value 1");
        assertThat(dummyManager.getActiveContextValue()).isEqualTo("Value 2");
    }

    @Test
    void getCapturedValue() {
        ContextSnapshot snapshot = ContextSnapshot.capture();
        assertThat(snapshot.getCapturedValue(dummyManager)).isNull();

        try (Context ignored = dummyManager.activate("Value 1")) {
            snapshot = ContextSnapshot.capture();
        }
        assertThat(snapshot.getCapturedValue(dummyManager)).isEqualTo("Value 1");
    }

    @Test
    void getCapturedValueManagerNull() {
        ContextSnapshot snapshot = ContextSnapshot.capture();
        assertThatThrownBy(() -> snapshot.getCapturedValue(null))
                .hasMessageContaining("contains no captured value");
    }

    @Test
    void getCapturedValue_errorDuringCapture() {
        ThrowingContextManager.onGet = new IllegalStateException("Error capturing value!");
        ThrowingContextManager throwingManager = new ThrowingContextManager();

        ContextSnapshot snapshot = assertDoesNotThrow(ContextSnapshot::capture);
        assertThat(snapshot).isNotNull();

        String result = assertDoesNotThrow(() -> snapshot.getCapturedValue(throwingManager));
        assertThat(result).isNull();
    }

}
