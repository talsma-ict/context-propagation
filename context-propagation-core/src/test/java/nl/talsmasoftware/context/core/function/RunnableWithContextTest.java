/*
 * Copyright 2016-2026 Talsma ICT
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Sjoerd Talsma
 */
class RunnableWithContextTest {

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
    void testRun() {
        new RunnableWithContext(snapshot, () -> {
        }).run();
        verify(snapshot).reactivate();
    }

    @Test
    void testRunWithoutSnapshot() {
        assertThatThrownBy(() -> new RunnableWithContext(null, () -> {
        }))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No context snapshot provided");
    }

    @Test
    void testRunWithoutSnapshotSupplier() {
        assertThatThrownBy(() -> new RunnableWithContext((Supplier<ContextSnapshot>) null, () -> {
        }, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No context snapshot supplier provided");
    }

    @Test
    void testRunWithSnapshotConsumer() throws InterruptedException {
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];
        DummyContext.setCurrentValue("Old value");

        Thread t = new Thread(new RunnableWithContext(snapshot,
                () -> DummyContext.setCurrentValue("New value"),
                s -> snapshotHolder[0] = s));
        t.start();
        t.join();

        assertThat(DummyContext.currentValue()).isEqualTo("Old value");
        try (ContextSnapshot.Reactivation reactivation = snapshotHolder[0].reactivate()) {
            assertThat(DummyContext.currentValue()).isEqualTo("New value");
        }

        verify(snapshot).reactivate();
    }

    @Test
    void testCloseReactivatedContextInCaseOfException() {
        ContextSnapshot.Reactivation reactivation = mock(ContextSnapshot.Reactivation.class);
        when(snapshot.reactivate()).thenReturn(reactivation);
        final RuntimeException expectedException = new RuntimeException("Whoops!");

        RunnableWithContext subject = new RunnableWithContext(snapshot, throwing(expectedException));
        assertThatThrownBy(subject::run).isSameAs(expectedException);

        verify(snapshot).reactivate();
        verify(reactivation).close();
    }

    static Runnable throwing(RuntimeException rte) {
        return () -> {
            throw rte;
        };
    }
}
