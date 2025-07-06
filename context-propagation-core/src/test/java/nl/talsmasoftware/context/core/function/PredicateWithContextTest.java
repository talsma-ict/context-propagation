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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
class PredicateWithContextTest {

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
    void testTest() {
        new PredicateWithContext<>(snapshot, input -> true).test("input");
        verify(snapshot).reactivate();
    }

    @Test
    void testTestWithoutSnapshot() {
        assertThatThrownBy(() -> new PredicateWithContext<>(null, input -> true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No context snapshot provided");
    }

    @Test
    void testTestWithoutSnapshotSupplier() {
        Supplier<ContextSnapshot> snapshotSupplier = null;
        Predicate<Object> predicate = input -> true;
        Consumer<ContextSnapshot> contextSnapshotConsumer = ctx -> {
        };

        assertThatThrownBy(() -> new PredicateWithContext<>(snapshotSupplier, predicate, contextSnapshotConsumer))
                .hasMessageContaining("No context snapshot supplier provided");
    }

    @Test
    void testTestWithSnapshotConsumer() throws InterruptedException {
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];
        DummyContext.setCurrentValue("Old value");

        Thread t = new Thread(() -> new PredicateWithContext<>(snapshot,
                input -> {
                    DummyContext.setCurrentValue("New value");
                    return true;
                },
                s -> snapshotHolder[0] = s).test("input"));
        t.start();
        t.join();

        assertThat(DummyContext.currentValue()).isEqualTo("Old value");
        try (ContextSnapshot.Reactivation ignored = snapshotHolder[0].reactivate()) {
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
        PredicateWithContext<Object> subject = new PredicateWithContext<>(snapshot, throwing(expectedException));

        assertThatThrownBy(() -> subject.test("input")).isSameAs(expectedException);

        verify(snapshot).reactivate();
        verify(reactivation).close();
    }

    @Test
    void testAndOtherNull() {
        PredicateWithContext<Object> subject = new PredicateWithContext<>(snapshot, input -> true);
        assertThatThrownBy(() -> subject.and(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("'and' <null>");
    }

    @Test
    void testAnd_singleContextSwitch() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        Predicate<String> predicate = Objects::nonNull;
        Predicate<String> and = String::isEmpty;

        AtomicInteger consumed = new AtomicInteger(0);
        Predicate<String> combined = new PredicateWithContext<>(snapshot, predicate, s -> consumed.incrementAndGet()).and(and);

        assertThat(combined.test("")).isTrue();
        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get()).isEqualTo(1);
    }

    @Test
    void testAnd_shortCircuit() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        Predicate<String> predicate = Objects::nonNull;
        @SuppressWarnings("unchecked")
        Predicate<String> and = mock(Predicate.class);

        AtomicInteger consumed = new AtomicInteger(0);
        Predicate<String> combined = new PredicateWithContext<>(snapshot, predicate, s -> consumed.incrementAndGet()).and(and);

        assertThat(combined.test(null)).isFalse();
        verifyNoMoreInteractions(and);

        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get()).isEqualTo(1);
    }

    @Test
    void testOrOtherNull() {
        PredicateWithContext<Object> subject = new PredicateWithContext<>(snapshot, input -> true);
        assertThatThrownBy(() -> subject.or(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("'or' <null>");
    }

    @Test
    void tesOr_singleContextSwitch() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        Predicate<String> predicate = Objects::isNull;
        Predicate<String> or = String::isEmpty;

        AtomicInteger consumed = new AtomicInteger(0);
        Predicate<String> combined = new PredicateWithContext<>(snapshot, predicate, s -> consumed.incrementAndGet()).or(or);

        assertThat(combined.test("")).isTrue();
        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get()).isEqualTo(1);
    }

    @Test
    void testOr_shortCircuit() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        Predicate<String> predicate = Objects::isNull;
        @SuppressWarnings("unchecked")
        Predicate<String> or = mock(Predicate.class);

        AtomicInteger consumed = new AtomicInteger(0);
        Predicate<String> combined = new PredicateWithContext<>(snapshot, predicate, s -> consumed.incrementAndGet()).or(or);

        assertThat(combined.test(null)).isTrue();
        verifyNoMoreInteractions(or);

        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get()).isEqualTo(1);
    }

    static <T> Predicate<T> throwing(RuntimeException rte) {
        return input -> {
            throw rte;
        };
    }
}
