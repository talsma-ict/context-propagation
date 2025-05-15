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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Sjoerd Talsma
 */
class FunctionWithContextTest {

    ContextSnapshot snapshot;
    Context context;

    @BeforeEach
    @AfterEach
    void clearDummyContext() {
        DummyContextManager.clearAllContexts();
    }

    @BeforeEach
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
        new FunctionWithContext<>(snapshot, Function.identity()).apply("input");
        verify(snapshot).reactivate();
    }

    @Test
    void testApplyWithoutSnapshot() {
        Function<Object, Object> identity = Function.identity();
        assertThatThrownBy(() -> new FunctionWithContext<>(null, identity))
                .hasMessageContaining("No context snapshot provided");
    }

    @Test
    void testApplyWithoutSnapshotSupplier() {
        Supplier<ContextSnapshot> nullSupplier = null;
        Function<Object, Object> identity = Function.identity();
        Consumer<ContextSnapshot> contextSnapshotConsumer = ctx -> {
        };

        assertThatThrownBy(() -> new FunctionWithContext<>(nullSupplier, identity, contextSnapshotConsumer))
                .hasMessageContaining("No context snapshot supplier provided");
    }

    @Test
    void testApplyWithSnapshotConsumer() throws InterruptedException {
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];
        DummyContext.setCurrentValue("Old value");

        Thread t = new Thread(() -> new FunctionWithContext<>(snapshot,
                input -> {
                    DummyContext.setCurrentValue("New value");
                    return input;
                },
                s -> snapshotHolder[0] = s).apply("input"));
        t.start();
        t.join();

        assertThat(DummyContext.currentValue(), is("Old value"));
        try (ContextSnapshot.Reactivation ignored = snapshotHolder[0].reactivate()) {
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

        FunctionWithContext<Object, Object> subject = new FunctionWithContext<>(snapshot, throwing(expectedException));
        assertThatThrownBy(() -> subject.apply("input")).isSameAs(expectedException);

        verify(snapshot).reactivate();
        verify(reactivation).close();
    }

    @Test
    void testComposeWithNull() {
        FunctionWithContext<Object, Object> subject = new FunctionWithContext<>(snapshot, Function.identity());
        assertThatThrownBy(() -> subject.compose(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("before function <null>");
    }

    @Test
    void testComposeWith_singleContextSwitch() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        Function<Integer, Integer> before = i -> i * 10;
        Function<Integer, Integer> function = i -> i + 3;
        AtomicInteger consumed = new AtomicInteger(0);

        Function<Integer, Integer> composed = new FunctionWithContext<>(snapshot, function, s -> consumed.incrementAndGet()).compose(before);

        assertThat(composed.apply(2), is((2 * 10) + 3));
        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get(), is(1));
    }

    @Test
    void testAndThen_singleContextSwitch() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        Function<Integer, Integer> after = i -> i * 10;
        Function<Integer, Integer> function = i -> i + 3;
        AtomicInteger consumed = new AtomicInteger(0);

        Function<Integer, Integer> composed = new FunctionWithContext<>(snapshot, function, s -> consumed.incrementAndGet()).andThen(after);

        assertThat(composed.apply(2), is((2 + 3) * 10));
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

    static <T, R> Function<T, R> throwing(RuntimeException rte) {
        return input -> {
            throw rte;
        };
    }
}
