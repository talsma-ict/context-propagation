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
package nl.talsmasoftware.context.core.function;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.dummy.DummyContext;
import nl.talsmasoftware.context.dummy.DummyContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
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
public class FunctionWithContextTest {

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
    public void testApply() {
        new FunctionWithContext<>(snapshot, Function.identity()).apply("input");
        verify(snapshot).reactivate();
    }

    @Test
    public void testApplyWithoutSnapshot() {
        try {
            new FunctionWithContext<>(null, Function.identity());
            fail("Exception expected");
        } catch (RuntimeException expected) {
            assertThat(expected, hasToString(containsString("No context snapshot provided")));
        }
    }

    @Test
    public void testApplyWithoutSnapshotSupplier() {
        try {
            new FunctionWithContext<>((Supplier<ContextSnapshot>) null, Function.identity(), context -> {
            });
            fail("Exception expected");
        } catch (RuntimeException expected) {
            assertThat(expected, hasToString(containsString("No context snapshot supplier provided")));
        }
    }

    @Test
    public void testApplyWithSnapshotConsumer() throws InterruptedException {
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];
        DummyContext.setCurrentValue("Old value");

        Thread t = new Thread(() -> new FunctionWithContext<>(snapshot,
                input -> {
                    DummyContext.setCurrentValue("New value");
                    return input;
                },
                snapshot -> snapshotHolder[0] = snapshot).apply("input"));
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
    public void testCloseReactivatedContextInCaseOfException() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        final RuntimeException expectedException = new RuntimeException("Whoops!");

        try {
            new FunctionWithContext<>(snapshot, throwing(expectedException)).apply("input");
            fail("Exception expected");
        } catch (RuntimeException rte) {
            assertThat(rte, is(sameInstance(expectedException)));
        }

        verify(snapshot).reactivate();
        verify(reactivation).close();
    }

    @Test
    public void testComposeWithNull() {
        try {
            new FunctionWithContext<>(snapshot, Function.identity()).compose(null);
            fail("Exception expected");
        } catch (NullPointerException expected) {
            assertThat(expected, hasToString(containsString("before function <null>")));
        }
    }

    @Test
    public void testComposeWith_singleContextSwitch() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        Function<Integer, Integer> before = i -> i * 10;
        Function<Integer, Integer> function = i -> i + 3;
        AtomicInteger consumed = new AtomicInteger(0);

        Function<Integer, Integer> composed = new FunctionWithContext<>(snapshot, function, snapshot -> consumed.incrementAndGet()).compose(before);

        assertThat(composed.apply(2), is((2 * 10) + 3));
        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
        assertThat(consumed.get(), is(1));
    }

    @Test
    public void testAndThen_singleContextSwitch() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        Function<Integer, Integer> after = i -> i * 10;
        Function<Integer, Integer> function = i -> i + 3;
        AtomicInteger consumed = new AtomicInteger(0);

        Function<Integer, Integer> composed = new FunctionWithContext<>(snapshot, function, snapshot -> consumed.incrementAndGet()).andThen(after);

        assertThat(composed.apply(2), is((2 + 3) * 10));
        verify(snapshot, times(1)).reactivate();
        verify(reactivation, times(1)).close();
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

    private static <IN, OUT> Function<IN, OUT> throwing(RuntimeException rte) {
        return input -> {
            throw rte;
        };
    }
}
