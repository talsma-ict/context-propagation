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
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Sjoerd Talsma
 */
public class UnaryOperatorWithContextTest {

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
        new UnaryOperatorWithContext<>(snapshot, input -> input).apply("input");
        verify(snapshot).reactivate();
    }

    @Test
    public void testApplyWithoutSnapshot() {
        try {
            new UnaryOperatorWithContext<>(null, input -> input);
            fail("Exception expected");
        } catch (RuntimeException expected) {
            assertThat(expected, hasToString(containsString("No context snapshot provided")));
        }
    }

    @Test
    public void testApplyWithoutSnapshotSupplier() {
        try {
            new UnaryOperatorWithContext<>((Supplier<ContextSnapshot>) null, input -> input, context -> {
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

        Thread t = new Thread(() -> new UnaryOperatorWithContext<>(snapshot,
                input -> {
                    DummyContextManager.setCurrentValue("New value");
                    return input;
                },
                snapshot -> snapshotHolder[0] = snapshot).apply("input"));
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
            new UnaryOperatorWithContext<>(snapshot, throwing(expectedException)).apply("input");
            fail("Exception expected");
        } catch (RuntimeException rte) {
            assertThat(rte, is(sameInstance(expectedException)));
        }

        verify(snapshot).reactivate();
        verify(context).close();
    }

    private static <T> UnaryOperator<T> throwing(RuntimeException rte) {
        return input -> {
            throw rte;
        };
    }
}
