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

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.dummy.DummyContext;
import nl.talsmasoftware.context.dummy.DummyContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class UnaryOperatorWithContextTest {

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
        snapshot = mock(ContextSnapshot.class);
        context = mock(Context.class);
    }

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(snapshot, context);
    }

    @Test
    void testApply() {
        new UnaryOperatorWithContext<>(snapshot, input -> input).apply("input");
        verify(snapshot).reactivate();
    }

    @Test
    void testApplyWithoutSnapshot() {
        assertThatThrownBy(() -> new UnaryOperatorWithContext<>(null, input -> input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No context snapshot provided");
    }

    @Test
    void testApplyWithoutSnapshotSupplier() {
        assertThatThrownBy(() -> new UnaryOperatorWithContext<>((Supplier<ContextSnapshot>) null, input -> input, ctx -> {
        }))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No context snapshot supplier provided");
    }

    @Test
    void testApplyWithSnapshotConsumer() throws InterruptedException {
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];
        DummyContext.setCurrentValue("Old value");

        Thread t = new Thread(() -> new UnaryOperatorWithContext<>(snapshot,
                input -> {
                    DummyContext.setCurrentValue("New value");
                    return input;
                },
                s -> snapshotHolder[0] = s)
                .apply("input"));
        t.start();
        t.join();

        assertThat(DummyContext.currentValue()).isEqualTo("Old value");
        try (ContextSnapshot.Reactivation reactivation = snapshotHolder[0].reactivate()) {
            assertThat(DummyContext.currentValue()).isEqualTo("New value");
        }
        assertThat(DummyContext.currentValue()).isEqualTo("Old value");

        verify(snapshot).reactivate();
    }

    @Test
    void testCloseReactivatedContextInCaseOfException() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        final RuntimeException expectedException = new RuntimeException("Whoops!");

        assertThatThrownBy(() -> new UnaryOperatorWithContext<String>(() -> snapshot, throwing(expectedException), null) {
        }.apply("input")).isSameAs(expectedException);

        verify(snapshot).reactivate();
        verify(reactivation).close();
    }

    static <T> UnaryOperator<T> throwing(RuntimeException rte) {
        return input -> {
            throw rte;
        };
    }
}
