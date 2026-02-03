/*
 * Copyright 2016-2026 Talsma ICT
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
package nl.talsmasoftware.context.core.function;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.dummy.DummyContext;
import nl.talsmasoftware.context.dummy.DummyContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class SupplierWithContextTest {

    ExecutorService unawareThreadpool;
    ContextSnapshot snapshot;
    Context context;

    @BeforeEach
    @AfterEach
    void clearDummyContext() {
        DummyContextManager.clearAllContexts();
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        unawareThreadpool = Executors.newCachedThreadPool();
        snapshot = mock(ContextSnapshot.class);
        context = mock(Context.class);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        verifyNoMoreInteractions(snapshot, context);
        unawareThreadpool.shutdown();
        unawareThreadpool.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void testGetNull() {
        assertThat(new SupplierWithContext<>(snapshot, () -> null).get()).isNull();
        verify(snapshot).reactivate();
    }

    @Test
    void testGetWithSnapshotConsumer() throws ExecutionException, InterruptedException {
        DummyContext.setCurrentValue("Old value");
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];

        Supplier<String> supplier = new SupplierWithContext<>(snapshot, () -> {
            try {
                return DummyContext.currentValue();
            } finally {
                DummyContext.setCurrentValue("New value");
            }
        }, s -> snapshotHolder[0] = s);

        Future<String> future = unawareThreadpool.submit(supplier::get);
        assertThat(future.get()).isNull();

        verify(snapshot).reactivate();
        assertThat(DummyContext.currentValue()).isEqualTo("Old value");
        try (ContextSnapshot.Reactivation reactivation = snapshotHolder[0].reactivate()) {
            assertThat(DummyContext.currentValue()).isEqualTo("New value");
        }
    }

}
