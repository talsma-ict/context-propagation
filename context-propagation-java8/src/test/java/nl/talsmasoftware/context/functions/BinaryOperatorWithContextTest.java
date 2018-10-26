/*
 * Copyright 2016-2018 Talsma ICT
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

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.DummyContextManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Sjoerd Talsma
 */
public class BinaryOperatorWithContextTest {

    private ContextSnapshot snapshot;
    private Context<Void> context;

    @Before
    @After
    public void clearDummyContext() {
        DummyContextManager.clear();
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        snapshot = mock(ContextSnapshot.class);
        context = mock(Context.class);
    }

    @After
    public void verifyMocks() {
        verifyNoMoreInteractions(snapshot, context);
    }

    @Test
    public void testApply() {
        new BinaryOperatorWithContext<>(snapshot, (a, b) -> b).apply("input1", "input2");
        verify(snapshot).reactivate();
    }

    @Test
    public void testApplyWithoutSnapshot() {
        try {
            new BinaryOperatorWithContext<>(null, (a, b) -> b);
            fail("Exception expected");
        } catch (RuntimeException expected) {
            assertThat(expected, hasToString(containsString("No context snapshot provided")));
        }
    }

    @Test
    public void testApplyWithoutSnapshotSupplier() {
        try {
            new BinaryOperatorWithContext<>((Supplier<ContextSnapshot>) null, (a, b) -> b, context -> {
            });
            fail("Exception expected");
        } catch (RuntimeException expected) {
            assertThat(expected, hasToString(containsString("No context snapshot supplier provided")));
        }
    }

    @Test
    public void testApplyWithSnapshotConsumer() throws InterruptedException {
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];
        DummyContextManager.setCurrentValue("Old value");

        Thread t = new Thread(() -> new BinaryOperatorWithContext<>(snapshot,
                (input1, input2) -> {
                    DummyContextManager.setCurrentValue("New value");
                    return input2;
                },
                snapshot -> snapshotHolder[0] = snapshot).apply("input1", "input2"));
        t.start();
        t.join();

        assertThat(DummyContextManager.currentValue(), is(Optional.of("Old value")));
        try (Context<Void> reactivation = snapshotHolder[0].reactivate()) {
            assertThat(DummyContextManager.currentValue(), is(Optional.of("New value")));
        }
        assertThat(DummyContextManager.currentValue(), is(Optional.of("Old value")));

        verify(snapshot).reactivate();
    }

    @Test
    public void testCloseReactivatedContextInCaseOfException() {
        when(snapshot.reactivate()).thenReturn(context);
        final RuntimeException expectedException = new RuntimeException("Whoops!");

        try {
            new BinaryOperatorWithContext<String>(() -> snapshot, throwing(expectedException), null) {
            }.apply("input1", "input2");
            fail("Exception expected");
        } catch (RuntimeException rte) {
            assertThat(rte, is(sameInstance(expectedException)));
        }

        verify(snapshot).reactivate();
        verify(context).close();
    }

    @Test
    public void testAndThenNull() {
        try {
            new BinaryOperatorWithContext<>(snapshot, (a, b) -> b).andThen(null);
            fail("Exception expected");
        } catch (NullPointerException expected) {
            assertThat(expected, hasToString(containsString("after function <null>")));
        }
    }

    @Test
    public void testAndThen_singleContextSwitch() {
        when(snapshot.reactivate()).thenReturn(context);
        UnaryOperator<Integer> after = i -> i + 100;
        BinaryOperator<Integer> function = (a, b) -> a * 10 + b * 5;
        AtomicInteger consumed = new AtomicInteger(0);

        BiFunction<Integer, Integer, Integer> composed =
                new BinaryOperatorWithContext<>(snapshot, function, snapshot -> consumed.incrementAndGet())
                        .andThen(after);

        assertThat(composed.apply(2, 3), is(20 + 15 + 100));
        verify(snapshot, times(1)).reactivate();
        verify(context, times(1)).close();
        assertThat(consumed.get(), is(1));
    }

    @Test
    public void testAndThenWithNull() {
        try {
            new FunctionWithContext<>(snapshot, Function.identity()).andThen(null);
            fail("Exception expected");
        } catch (NullPointerException expected) {
            assertThat(expected, hasToString(containsString("after function <null>")));
        }
    }

    private static <T> BinaryOperator<T> throwing(RuntimeException rte) {
        return (input1, input2) -> {
            throw rte;
        };
    }
}
