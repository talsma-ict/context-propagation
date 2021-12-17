/*
 * Copyright 2016-2021 Talsma ICT
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
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.DummyContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static nl.talsmasoftware.context.DummyContextManager.currentValue;
import static nl.talsmasoftware.context.DummyContextManager.setCurrentValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Sjoerd Talsma
 */
public class ConsumerWithContextTest {
    private ExecutorService unawareThreadpool;
    private ContextSnapshot snapshot;
    private Context<Void> context;

/*
    @BeforeAll
    public static void initLogback() {
        */
/* Initialize SLF4J bridge. This re-routes logging through java.util.logging to SLF4J. *//*

        java.util.logging.LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
        LoggerFactory.getILoggerFactory();
    }
*/

    @BeforeEach
    @AfterEach
    public void clearDummyContext() {
        DummyContextManager.clear();
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        unawareThreadpool = Executors.newCachedThreadPool();
        snapshot = mock(ContextSnapshot.class);
        context = mock(Context.class);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        verifyNoMoreInteractions(snapshot, context);
        unawareThreadpool.shutdown();
        unawareThreadpool.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void testAcceptNull() {
        Object[] accepted = new Object[1];
        new ConsumerWithContext<>(snapshot, val -> accepted[0] = val).accept(null);
        assertThat(accepted[0], is(nullValue()));
        verify(snapshot).reactivate();
    }

    @Test
    public void testAcceptWithSnapshotConsumer() throws InterruptedException {
        setCurrentValue("Old value");
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];

        ConsumerWithContext<String> consumer = new ConsumerWithContext<>(
                ContextManagers.createContextSnapshot(),
                val -> {
                    assertThat("Context must propagate into thread", currentValue(), is(Optional.of("Old value")));
                    setCurrentValue(val);
                    assertThat("Context changed in background thread", currentValue(), is(Optional.of(val)));
                    trySleep(250);
                },
                s -> snapshotHolder[0] = s);

        Thread t = new Thread(() -> consumer.accept("New value"));
        t.start();
        assertThat("Setting context in other thread musn't impact caller", currentValue(), is(Optional.of("Old value")));
        t.join(); // Block and trigger assertions

        assertThat("Setting context in other thread musn't impact caller", currentValue(), is(Optional.of("Old value")));
        assertThat("Snapshot consumer must be called", snapshotHolder[0], is(notNullValue()));

        t = new Thread(() -> {
            try (Context<Void> reactivation = snapshotHolder[0].reactivate()) {
                assertThat("Thread context must propagate", currentValue(), is(Optional.of("New value")));
            }
        });
        t.start();
        t.join(); // Block and trigger assertions
    }

    @Test
    public void testAndThen() throws InterruptedException {
        setCurrentValue("Old value");
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];

        Consumer<String> consumer = new ConsumerWithContext<String>(
                ContextManagers.createContextSnapshot(),
                val -> setCurrentValue(val + ", " + currentValue().orElse("NO VALUE")),
                s -> snapshotHolder[0] = s)
                .andThen(val -> setCurrentValue(val.toUpperCase() + ", " + currentValue().orElse("NO VALUE")));

        Thread t = new Thread(() -> consumer.accept("New value"));
        t.start();
        t.join();

        assertThat(currentValue(), is(Optional.of("Old value")));
        try (Context<Void> reactivated = snapshotHolder[0].reactivate()) {
            assertThat(currentValue(), is(Optional.of("NEW VALUE, New value, Old value")));
        }
    }

    private static void trySleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrupted while sleeping");
        }
    }

}
