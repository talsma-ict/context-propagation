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
import nl.talsmasoftware.context.api.ContextSnapshot.Reactivation;
import nl.talsmasoftware.context.dummy.DummyContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static java.lang.String.format;
import static nl.talsmasoftware.context.dummy.DummyContext.currentValue;
import static nl.talsmasoftware.context.dummy.DummyContext.setCurrentValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Sjoerd Talsma
 */
class BiConsumerWithContextTest {
    private ExecutorService unawareThreadpool;
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
        unawareThreadpool = Executors.newCachedThreadPool();
        snapshot = mock(ContextSnapshot.class);
        context = mock(Context.class);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        verifyNoMoreInteractions(snapshot, context);
        unawareThreadpool.shutdown();
        unawareThreadpool.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void testAcceptNulls() {
        Object[] accepted = new Object[2];
        new BiConsumerWithContext<>(snapshot, (a, b) -> {
            accepted[0] = a;
            accepted[1] = b;
        }).accept(null, null);
        assertThat(accepted[0], is(nullValue()));
        assertThat(accepted[1], is(nullValue()));
        verify(snapshot).reactivate();
    }

    @Test
    void testAcceptWithSnapshotConsumer() throws InterruptedException {
        setCurrentValue("Old value");
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];

        CountDownLatch latch = new CountDownLatch(1);
        BiConsumer<String, String> consumer = new BiConsumerWithContext<>(
                ContextSnapshot.capture(),
                (a, b) -> {
                    try {
                        assertThat("Context must propagate into thread", currentValue(), is(Optional.of("Old value")));
                        String newValue = format("%s %s", a, b);
                        setCurrentValue(newValue);
                        assertThat("Context changed in background thread", currentValue(), is(Optional.of(newValue)));
                        latch.await();
                    } catch (InterruptedException e) {
                        fail(e);
                    }
                },
                s -> snapshotHolder[0] = s);

        Thread t = new Thread(() -> consumer.accept("New", "value"));
        t.start();
        assertThat("Setting context in other thread musn't impact caller", currentValue(), is("Old value"));

        latch.countDown();
        t.join(); // Block and trigger assertions
        assertThat("Setting context in other thread musn't impact caller", currentValue(), is("Old value"));
        assertThat("Snapshot consumer must be called", snapshotHolder[0], is(notNullValue()));

        t = new Thread(() -> {
            try (Reactivation reactivation = snapshotHolder[0].reactivate()) {
                assertThat("Thread context must propagate", currentValue(), is("New value"));
            }
        });
        t.start();
        t.join(); // Block and trigger assertions
    }

    @Test
    void testAndThen() throws InterruptedException {
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];

        setCurrentValue("Old value");
        BiConsumer<String, String> consumer1 = new BiConsumerWithContext<>(
                ContextSnapshot.capture(),
                (a, b) -> setCurrentValue(a + " " + b + ", " + Optional.ofNullable(currentValue()).orElse("NO VALUE")),
                s -> snapshotHolder[0] = s);
        BiConsumer<String, String> consumer2 = consumer1.andThen(
                (a, b) -> setCurrentValue(a.toUpperCase() + " " + b.toLowerCase() + ", " + Optional.ofNullable(currentValue()).orElse("NO VALUE")));

        Thread t = new Thread(() -> consumer2.accept("New", "value"));
        t.start();
        t.join();

        assertThat(currentValue(), is("Old value"));
        try (Reactivation reactivated = snapshotHolder[0].reactivate()) {
            assertThat(currentValue(), is("NEW value, New value, Old value"));
        }
    }
}
