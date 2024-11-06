/*
 * Copyright 2016-2024 Talsma ICT
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
package nl.talsmasoftware.context.functions;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.dummy.DummyContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Sjoerd Talsma
 */
public class PredicateWithContextTest {

    private ContextSnapshot snapshot;
    private Context<Void> context;

    @BeforeEach
    @AfterEach
    public void clearDummyContext() {
        DummyContextManager.clearAllContexts();
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        snapshot = mock(ContextSnapshot.class);
        context = mock(Context.class);
    }

    @AfterEach
    public void verifyMocks() {
        verifyNoMoreInteractions(snapshot, context);
    }

    @Test
    public void testTest() {
        new PredicateWithContext<>(snapshot, input -> true).test("input");
        verify(snapshot).reactivate();
    }

    @Test
    public void testTestWithoutSnapshot() {
        try {
            new PredicateWithContext<>(null, input -> true);
            fail("Exception expected");
        } catch (RuntimeException expected) {
            assertThat(expected, hasToString(containsString("No context snapshot provided")));
        }
    }

    @Test
    public void testTestWithoutSnapshotSupplier() {
        try {
            new PredicateWithContext<>((Supplier<ContextSnapshot>) null, input -> true, context -> {
            });
            fail("Exception expected");
        } catch (RuntimeException expected) {
            assertThat(expected, hasToString(containsString("No context snapshot supplier provided")));
        }
    }

    @Test
    public void testTestWithSnapshotConsumer() throws InterruptedException, IOException {
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];
        DummyContextManager.setCurrentValue("Old value");

        Thread t = new Thread(() -> new PredicateWithContext<>(snapshot,
                input -> {
                    DummyContextManager.setCurrentValue("New value");
                    return true;
                },
                snapshot -> snapshotHolder[0] = snapshot).test("input"));
        t.start();
        t.join();

        assertThat(DummyContextManager.currentValue(), is(Optional.of("Old value")));
        try (Closeable reactivation = snapshotHolder[0].reactivate()) {
            assertThat(DummyContextManager.currentValue(), is(Optional.of("New value")));
        }
        assertThat(DummyContextManager.currentValue(), is(Optional.of("Old value")));

        verify(snapshot).reactivate();
    }

    @Test
    public void testCloseReactivatedContextInCaseOfException() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        final RuntimeException expectedException = new RuntimeException("Whoops!");

        try {
            new PredicateWithContext<>(snapshot, throwing(expectedException)).test("input");
            fail("Exception expected");
        } catch (RuntimeException rte) {
            assertThat(rte, is(sameInstance(expectedException)));
        }

        verify(snapshot).reactivate();
        verify(reactivation).close();
    }

    @Test
    public void testAndOtherNull() {
        try {
            new PredicateWithContext<>(snapshot, input -> true).and(null);
            fail("Exception expected");
        } catch (NullPointerException expected) {
            assertThat(expected, hasToString(containsString("'and' <null>")));
        }
    }

    @Test
    public void testAnd_singleContextSwitch() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        Predicate<String> predicate = Objects::nonNull;
        Predicate<String> and = String::isEmpty;

        AtomicInteger consumed = new AtomicInteger(0);
        Predicate<String> combined = new PredicateWithContext<>(snapshot, predicate, snapshot -> consumed.incrementAndGet()).and(and);

        assertThat(combined.test(""), is(true));
        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get(), is(1));
    }

    @Test
    public void testAnd_shortCircuit() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        Predicate<String> predicate = Objects::nonNull;
        @SuppressWarnings("unchecked")
        Predicate<String> and = mock(Predicate.class);

        AtomicInteger consumed = new AtomicInteger(0);
        Predicate<String> combined = new PredicateWithContext<>(snapshot, predicate, snapshot -> consumed.incrementAndGet()).and(and);

        assertThat(combined.test(null), is(false));
        verifyNoMoreInteractions(and);

        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get(), is(1));
    }

    @Test
    public void testOrOtherNull() {
        try {
            new PredicateWithContext<>(snapshot, input -> true).or(null);
            fail("Exception expected");
        } catch (NullPointerException expected) {
            assertThat(expected, hasToString(containsString("'or' <null>")));
        }
    }

    @Test
    public void tesOr_singleContextSwitch() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        Predicate<String> predicate = Objects::isNull;
        Predicate<String> or = String::isEmpty;

        AtomicInteger consumed = new AtomicInteger(0);
        Predicate<String> combined = new PredicateWithContext<>(snapshot, predicate, snapshot -> consumed.incrementAndGet()).or(or);

        assertThat(combined.test(""), is(true));
        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get(), is(1));
    }

    @Test
    public void testOr_shortCircuit() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        Predicate<String> predicate = Objects::isNull;
        @SuppressWarnings("unchecked")
        Predicate<String> or = mock(Predicate.class);

        AtomicInteger consumed = new AtomicInteger(0);
        Predicate<String> combined = new PredicateWithContext<>(snapshot, predicate, snapshot -> consumed.incrementAndGet()).or(or);

        assertThat(combined.test(null), is(true));
        verifyNoMoreInteractions(or);

        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get(), is(1));
    }

    private static <T> Predicate<T> throwing(RuntimeException rte) {
        return input -> {
            throw rte;
        };
    }
}
