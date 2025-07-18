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
package nl.talsmasoftware.context.core.function;

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
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Sjoerd Talsma
 */
class BooleanSupplierWithContextTest {

    private ExecutorService unawareThreadpool;

    @BeforeEach
    @AfterEach
    void clearDummyContext() {
        DummyContextManager.clearAllContexts();
    }

    @BeforeEach
    void setUp() {
        unawareThreadpool = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        unawareThreadpool.shutdown();
        unawareThreadpool.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void testConstructWithoutSnapshot() {
        assertThatThrownBy(() -> new BooleanSupplierWithContext(null, () -> true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No context snapshot provided");
    }

    @Test
    void testConstructWithoutSnapshotSupplier() {
        assertThatThrownBy(() -> new BooleanSupplierWithContext((Supplier<ContextSnapshot>) null, () -> true, snapshot -> {
        }))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No context snapshot supplier provided");
    }

    @Test
    void testGetAsBooleanWithSnapshotConsumer() throws ExecutionException, InterruptedException {
        DummyContext.setCurrentValue("true");
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];

        BooleanSupplier supplier = new BooleanSupplierWithContext(ContextSnapshot.capture(), () -> {
            try {
                return Boolean.parseBoolean(DummyContext.currentValue());
            } finally {
                DummyContext.setCurrentValue("false");
            }
        }, s -> snapshotHolder[0] = s);

        Future<Boolean> future = unawareThreadpool.submit(supplier::getAsBoolean);
        assertThat(future.get()).isTrue();

        assertThat(DummyContext.currentValue()).isEqualTo("true");
        try (ContextSnapshot.Reactivation reactivation = snapshotHolder[0].reactivate()) {
            assertThat(DummyContext.currentValue()).isEqualTo("false");
        }
        assertThat(DummyContext.currentValue()).isEqualTo("true");
    }

}
