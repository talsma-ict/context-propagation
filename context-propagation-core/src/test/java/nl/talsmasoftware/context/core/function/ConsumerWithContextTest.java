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
import nl.talsmasoftware.context.dummy.DummyContextManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static nl.talsmasoftware.context.dummy.DummyContext.assertCurrentValue;
import static nl.talsmasoftware.context.dummy.DummyContext.currentValue;
import static nl.talsmasoftware.context.dummy.DummyContext.setCurrentValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Sjoerd Talsma
 */
class ConsumerWithContextTest {
    private ExecutorService unawareThreadPool;
    private ContextSnapshot snapshot;
    private Context context;

    @BeforeEach
    @AfterEach
    void clearDummyContext() {
        DummyContextManager.clearAllContexts();
    }

    @BeforeEach
    void setUp() {
        unawareThreadPool = Executors.newCachedThreadPool();
        snapshot = mock(ContextSnapshot.class);
        context = mock(Context.class);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        verifyNoMoreInteractions(snapshot, context);
        unawareThreadPool.shutdown();
        unawareThreadPool.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void testAcceptNull() {
        Object[] accepted = new Object[1];
        new ConsumerWithContext<>(snapshot, val -> accepted[0] = val).accept(null);
        assertThat(accepted[0], is(nullValue()));
        verify(snapshot).reactivate();
    }

    @Test
    void testAcceptWithSnapshotConsumer() throws InterruptedException {
        setCurrentValue("Old value");
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];

        CountDownLatch latch = new CountDownLatch(1);
        ConsumerWithContext<String> consumer = new ConsumerWithContext<>(
                ContextSnapshot.capture(),
                val -> {
                    try {
                        assertCurrentValue().isEqualTo("Old value");
                        setCurrentValue(val);
                        assertCurrentValue().isEqualTo(val);
                        latch.await();
                    } catch (InterruptedException e) {
                        fail(e);
                    }
                },
                s -> snapshotHolder[0] = s);

        Thread t = new Thread(() -> consumer.accept("New value"));
        t.start();
        assertCurrentValue().isEqualTo("Old value"); // setting context in another thread mustn't impact caller
        latch.countDown();
        t.join(); // Block and trigger assertions

        assertCurrentValue().isEqualTo("Old value");
        Assertions.assertThat(snapshotHolder[0]).isNotNull();

        t = new Thread(() -> {
            try (ContextSnapshot.Reactivation ignored = snapshotHolder[0].reactivate()) {
                assertCurrentValue().isEqualTo("New value");
            }
        });
        t.start();
        t.join(); // Block and trigger assertions
    }

    @Test
    void testAndThen() throws InterruptedException {
        setCurrentValue("Old value");
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];

        Consumer<String> consumer = new ConsumerWithContext<String>(
                ContextSnapshot.capture(),
                val -> setCurrentValue(val + ", " + Optional.ofNullable(currentValue()).orElse("NO VALUE")),
                s -> snapshotHolder[0] = s)
                .andThen(val -> setCurrentValue(val.toUpperCase() + ", " + Optional.ofNullable(currentValue()).orElse("NO VALUE")));

        Thread t = new Thread(() -> consumer.accept("New value"));
        t.start();
        t.join();

        assertThat(currentValue(), is("Old value"));
        try (ContextSnapshot.Reactivation ignored = snapshotHolder[0].reactivate()) {
            assertThat(currentValue(), is("NEW VALUE, New value, Old value"));
        }
    }
}
