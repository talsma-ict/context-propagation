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
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Sjoerd Talsma
 */
class BiFunctionWithContextTest {

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
        new BiFunctionWithContext<>(snapshot, (a, b) -> b).apply("input1", "input2");
        verify(snapshot).reactivate();
    }

    @Test
    void testApplyWithoutSnapshot() {
        try {
            new BiFunctionWithContext<>(null, (a, b) -> b);
            fail("Exception expected");
        } catch (RuntimeException expected) {
            assertThat(expected, hasToString(containsString("No context snapshot provided")));
        }
    }

    @Test
    void testApplyWithoutSnapshotSupplier() {
        try {
            new BiFunctionWithContext<>((Supplier<ContextSnapshot>) null, (a, b) -> b, ctx -> {
            });
            fail("Exception expected");
        } catch (RuntimeException expected) {
            assertThat(expected, hasToString(containsString("No context snapshot supplier provided")));
        }
    }

    @Test
    void testApplyWithSnapshotConsumer() throws InterruptedException {
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];
        DummyContext.setCurrentValue("Old value");

        Thread t = new Thread(() -> new BiFunctionWithContext<>(snapshot,
                (input1, input2) -> {
                    DummyContext.setCurrentValue("New value");
                    return input2;
                },
                s -> snapshotHolder[0] = s).apply("input1", "input2"));
        t.start();
        t.join();

        assertThat(DummyContext.currentValue(), is("Old value"));
        try (ContextSnapshot.Reactivation reactivation = snapshotHolder[0].reactivate()) {
            assertThat(DummyContext.currentValue(), is("New value"));
        }
        assertThat(DummyContext.currentValue(), is("Old value"));

        verify(snapshot).reactivate();
    }

    @Test
    void testCloseReactivatedContextInCaseOfException() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        final RuntimeException expectedException = new RuntimeException("Whoops!");

        BiFunctionWithContext<Object, Object, Object> subject = new BiFunctionWithContext<>(snapshot, throwing(expectedException));
        assertThatThrownBy(() -> subject.apply("input1", "input2")).isSameAs(expectedException);

        verify(snapshot).reactivate();
        verify(reactivation).close();
    }

    @Test
    void testAndThenNull() {
        BiFunctionWithContext<Object, Object, Object> subject = new BiFunctionWithContext<>(snapshot, (a, b) -> b);
        assertThatThrownBy(() -> subject.andThen(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("after function <null>");
    }

    @Test
    void testAndThen_singleContextSwitch() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        Function<Integer, Integer> after = i -> i + 100;
        BiFunction<Integer, Integer, Integer> function = (a, b) -> a * 10 + b * 5;
        AtomicInteger consumed = new AtomicInteger(0);

        BiFunction<Integer, Integer, Integer> composed =
                new BiFunctionWithContext<>(snapshot, function, s -> consumed.incrementAndGet())
                        .andThen(after);

        assertThat(composed.apply(2, 3), is(20 + 15 + 100));
        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get(), is(1));
    }

    @Test
    void testAndThenWithNull() {
        FunctionWithContext<Object, Object> subject = new FunctionWithContext<>(snapshot, Function.identity());
        assertThatThrownBy(() -> subject.andThen(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("after function <null>");
    }

    private static <IN1, IN2, OUT> BiFunction<IN1, IN2, OUT> throwing(RuntimeException rte) {
        return (input1, input2) -> {
            throw rte;
        };
    }
}
