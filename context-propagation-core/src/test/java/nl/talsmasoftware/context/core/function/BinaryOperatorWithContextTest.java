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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Sjoerd Talsma
 */
class BinaryOperatorWithContextTest {

    private ContextSnapshot snapshot;
    private Context context;

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
        new BinaryOperatorWithContext<>(snapshot, (a, b) -> b).apply("input1", "input2");
        verify(snapshot).reactivate();
    }

    @Test
    void testApplyWithoutSnapshot() {
        assertThatThrownBy(() -> new BinaryOperatorWithContext<>(null, (a, b) -> b))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No context snapshot provided");
    }

    @Test
    void testApplyWithoutSnapshotSupplier() {
        assertThatThrownBy(() -> new BinaryOperatorWithContext<>((Supplier<ContextSnapshot>) null, (a, b) -> b, c -> {
        }))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No context snapshot supplier provided");
    }

    @Test
    void testApplyWithSnapshotConsumer() throws InterruptedException {
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];
        DummyContext.setCurrentValue("Old value");

        Thread t = new Thread(() -> new BinaryOperatorWithContext<>(snapshot,
                (input1, input2) -> {
                    DummyContext.setCurrentValue("New value");
                    return input2;
                },
                s -> snapshotHolder[0] = s).apply("input1", "input2"));
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

        BinaryOperatorWithContext<String> subject = new BinaryOperatorWithContext<>(() -> snapshot, throwing(expectedException), null) {
        };
        assertThatThrownBy(() -> subject.apply("input1", "input2")).isSameAs(expectedException);

        verify(snapshot).reactivate();
        verify(reactivation).close();
    }

    @Test
    void testAndThenNull() {
        BinaryOperatorWithContext<Object> subject = new BinaryOperatorWithContext<>(snapshot, (a, b) -> b);
        assertThatThrownBy(() -> subject.andThen(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("after function <null>");
    }

    @Test
    void testAndThen_singleContextSwitch() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        UnaryOperator<Integer> after = i -> i + 100;
        BinaryOperator<Integer> function = (a, b) -> a * 10 + b * 5;
        AtomicInteger consumed = new AtomicInteger(0);

        BiFunction<Integer, Integer, Integer> composed =
                new BinaryOperatorWithContext<>(snapshot, function, s -> consumed.incrementAndGet())
                        .andThen(after);

        assertThat(composed.apply(2, 3)).isEqualTo(20 + 15 + 100);
        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get()).isEqualTo(1);
    }

    @Test
    void testAndThenWithNull() {
        FunctionWithContext<Object, Object> subject = new FunctionWithContext<>(snapshot, Function.identity());
        assertThatThrownBy(() -> subject.andThen(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("after function <null>");
    }

    private static <T> BinaryOperator<T> throwing(RuntimeException rte) {
        return (input1, input2) -> {
            throw rte;
        };
    }
}
