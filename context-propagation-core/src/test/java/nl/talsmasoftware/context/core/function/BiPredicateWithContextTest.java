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
import java.util.function.BiPredicate;
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
class BiPredicateWithContextTest {

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
    void testTest() {
        new BiPredicateWithContext<>(snapshot, (a, b) -> true).test("input1", "input2");
        verify(snapshot).reactivate();
    }

    @Test
    void testTestWithoutSnapshot() {
        try {
            new BiPredicateWithContext<>(null, (a, b) -> true);
            fail("Exception expected");
        } catch (RuntimeException expected) {
            assertThat(expected, hasToString(containsString("No context snapshot provided")));
        }
    }

    @Test
    void testTestWithoutSnapshotSupplier() {
        try {
            new BiPredicateWithContext<>((Supplier<ContextSnapshot>) null, (a, b) -> true, c -> {
            });
            fail("Exception expected");
        } catch (RuntimeException expected) {
            assertThat(expected, hasToString(containsString("No context snapshot supplier provided")));
        }
    }

    @Test
    void testTestWithSnapshotConsumer() throws InterruptedException {
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];
        DummyContext.setCurrentValue("Old value");

        Thread t = new Thread(() -> new BiPredicateWithContext<>(snapshot,
                (a, b) -> {
                    DummyContext.setCurrentValue("New value");
                    return true;
                },
                s -> snapshotHolder[0] = s).test("input1", "input2"));
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

        BiPredicateWithContext<Object, Object> subject = new BiPredicateWithContext<>(snapshot, throwing(expectedException));
        assertThatThrownBy(() -> subject.test("input1", "input2")).isSameAs(expectedException);

        verify(snapshot).reactivate();
        verify(reactivation).close();
    }

    @Test
    void testAndOtherNull() {
        BiPredicateWithContext<Object, Object> subject = new BiPredicateWithContext<>(snapshot, (a, b) -> true);
        assertThatThrownBy(() -> subject.and(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("'and' <null>");
    }

    @Test
    void testAnd_singleContextSwitch() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        BiPredicate<String, String> predicate = (a, b) -> a != null && b != null;
        BiPredicate<String, String> and = (a, b) -> a.isEmpty() || b.isEmpty();

        AtomicInteger consumed = new AtomicInteger(0);
        BiPredicate<String, String> combined =
                new BiPredicateWithContext<>(snapshot, predicate, s -> consumed.incrementAndGet()).and(and);

        assertThat(combined.test("", ""), is(true));
        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get(), is(1));
    }

    @Test
    void testAnd_shortCircuit() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        BiPredicate<String, String> predicate = (a, b) -> a != null && b != null;
        @SuppressWarnings("unchecked")
        BiPredicate<String, String> and = mock(BiPredicate.class);

        AtomicInteger consumed = new AtomicInteger(0);
        BiPredicate<String, String> combined =
                new BiPredicateWithContext<>(snapshot, predicate, s -> consumed.incrementAndGet()).and(and);

        assertThat(combined.test(null, null), is(false));
        verifyNoMoreInteractions(and);

        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get(), is(1));
    }

    @Test
    void testOrOtherNull() {
        BiPredicateWithContext<Object, Object> subject = new BiPredicateWithContext<>(snapshot, (a, b) -> true);
        assertThatThrownBy(() -> subject.or(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("'or' <null>");
    }

    @Test
    void tesOr_singleContextSwitch() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        BiPredicate<String, String> predicate = (a, b) -> a == null || b == null;
        BiPredicate<String, String> or = (a, b) -> a.isEmpty() || b.isEmpty();

        AtomicInteger consumed = new AtomicInteger(0);
        BiPredicate<String, String> combined =
                new BiPredicateWithContext<>(snapshot, predicate, s -> consumed.incrementAndGet()).or(or);

        assertThat(combined.test("", ""), is(true));
        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get(), is(1));
    }

    @Test
    void testOr_shortCircuit() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        BiPredicate<String, String> predicate = (a, b) -> a == null || b == null;
        @SuppressWarnings("unchecked")
        BiPredicate<String, String> or = mock(BiPredicate.class);

        AtomicInteger consumed = new AtomicInteger(0);
        BiPredicate<String, String> combined =
                new BiPredicateWithContext<>(snapshot, predicate, s -> consumed.incrementAndGet()).or(or);

        assertThat(combined.test(null, null), is(true));
        verifyNoMoreInteractions(or);

        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get(), is(1));
    }

    private static <T, U> BiPredicate<T, U> throwing(RuntimeException rte) {
        return (t, u) -> {
            throw rte;
        };
    }
}
