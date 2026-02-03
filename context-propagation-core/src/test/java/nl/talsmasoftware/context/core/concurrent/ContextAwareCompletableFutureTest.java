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
package nl.talsmasoftware.context.core.concurrent;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.dummy.DummyContext;
import nl.talsmasoftware.context.dummy.DummyContextManager;
import org.assertj.core.api.StringAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static nl.talsmasoftware.context.core.concurrent.ContextAwareCompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * @author Sjoerd Talsma
 */
class ContextAwareCompletableFutureTest {
    static final Random RND = new Random();
    static final DummyContextManager manager = new DummyContextManager();
    static final ExecutorService contextUnawareThreadpool = Executors.newCachedThreadPool();

    @BeforeEach
    @AfterEach
    void clearDummyContext() {
        DummyContextManager.clearAllContexts();
    }

    static StringAssert assertActiveContextValue() {
        return new StringAssert(manager.getActiveContextValue()).as("Active context value");
    }

    static void assertContext(String expectedValue) {
        assertActiveContextValue().isEqualTo(expectedValue);
    }

    @SuppressWarnings("java:S2925") // Thread.sleep is used here to 'simulate' random work between 50ms...1s
    static Supplier<String> stringSupplier(String name, String requiredContext) {
        return () -> {
            assertContext(requiredContext);
            DummyContext.setCurrentValue(name); // intentionally not closed here
            assertContext(name);
            assertDoesNotThrow(() -> Thread.sleep(50 + RND.nextInt(200)));
            return String.format("%s (Thread: %s)", name, Thread.currentThread().getName());
        };
    }

    @Test
    void testDefaultConstructor() throws ExecutionException, InterruptedException {
        Context ctx = manager.activate("foo");
        CompletableFuture<String> future1 = new ContextAwareCompletableFuture<>(); // should have a new snapshot with foo
        ctx.close();

        CompletableFuture<String> future2 = future1.thenApply(value -> manager.getActiveContextValue() + value);
        assertThat(manager.getActiveContextValue()).isNull();
        assertThat(future1.isDone()).isFalse();
        assertThat(future2.isDone()).isFalse();

        assertThat(future1.complete("bar")).isTrue();
        assertThat(future1.isDone()).isTrue();
        assertThat(future2.isDone()).isTrue();
        assertThat(future2.get()).isEqualTo("foobar");
    }

    @Test
    void testSupplyAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Vincent Vega")) {
            Future<String> future = supplyAsync(DummyContext::currentValue);
            assertThat(future.get()).isEqualTo("Vincent Vega");
        }
    }

    @Test
    void testSupplyAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Marcellus Wallace")) {
            Future<String> future = supplyAsync(DummyContext::currentValue, contextUnawareThreadpool);
            assertThat(future.get()).isEqualTo("Marcellus Wallace");
        }
    }

    @Test
    void testSupplyAsync_executor_snapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Vincent Vega")) {
            ContextSnapshot snapshot = ContextSnapshot.capture();
            DummyContext.setCurrentValue("Jules Winnfield");
            assertContext("Jules Winnfield");

            Future<String> future = supplyAsync(DummyContext::currentValue, contextUnawareThreadpool, snapshot);
            assertThat(future.get()).isEqualTo("Vincent Vega");
        }
    }

    @Test
    void testSupplyAsync_executor_snapshot_takeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Vincent Vega")) {
            ContextSnapshot snapshot = ContextSnapshot.capture();
            DummyContext.setCurrentValue("Jules Winnfield");
            assertContext("Jules Winnfield");

            ContextAwareCompletableFuture<String> future = supplyAsync(() -> {
                try {
                    return DummyContext.currentValue();
                } finally {
                    DummyContext.setCurrentValue("Marcellus Wallace");
                }
            }, contextUnawareThreadpool, snapshot, true);

            assertThat(future.get()).isEqualTo("Vincent Vega");
            assertThat(future.thenApply(x -> DummyContext.currentValue()).get()).isEqualTo("Marcellus Wallace");
        }
    }

    @Test
    void testRunAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Mia Wallace")) {
            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(() -> assertContext("Mia Wallace"));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testRunAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Jimmie")) {
            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(() -> assertContext("Jimmie"), contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testRunAsync_executor_snapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Pumpkin")) {
            ContextSnapshot snapshot = ContextSnapshot.capture();
            DummyContext.setCurrentValue("Honey Bunny");
            assertContext("Honey Bunny");

            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(() -> assertContext("Pumpkin"), contextUnawareThreadpool, snapshot);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testRunAsync_executor_snapshot_takeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ingored = manager.activate("Pumpkin")) {
            ContextSnapshot snapshot = ContextSnapshot.capture();
            DummyContext.setCurrentValue("Honey Bunny");
            assertContext("Honey Bunny");

            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(() -> {
                assertContext("Pumpkin");
                DummyContext.setCurrentValue("Bad Motherfucker");
            }, contextUnawareThreadpool, snapshot, true);
            future.get(); // trigger asynchronous assertion and makes sure the function already ended.
            future.thenRun(() -> assertContext("Bad Motherfucker")).get();
        }
    }

    @Test
    void testThenApply() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Jimmie")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Bonnie"))
                    .thenApply(voidvalue -> DummyContext.currentValue());
            assertThat(future.get()).isEqualTo("Jimmie");
        }

        try (Context ignored = manager.activate("Jimmie")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Bonnie"), null, null, true)
                    .thenApply(voidvalue -> DummyContext.currentValue());
            assertThat(future.get()).isEqualTo("Bonnie");
        }
    }

    @Test
    void testThenApplyAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Butch")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Esmerelda Villalobos"))
                    .thenApplyAsync(voidvalue -> DummyContext.currentValue());
            assertThat(future.get()).isEqualTo("Butch");
        }

        try (Context ignored = manager.activate("Butch")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Esmerelda Villalobos"), null, null, true)
                    .thenApplyAsync(voidvalue -> DummyContext.currentValue());
            assertThat(future.get()).isEqualTo("Esmerelda Villalobos");
        }
    }

    @Test
    void testThenApplyAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Maynard")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Zed"))
                    .thenApplyAsync(voidvalue -> DummyContext.currentValue(), contextUnawareThreadpool);
            assertThat(future.get()).isEqualTo("Maynard");
        }

        try (Context ignored = manager.activate("Maynard")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Zed"), null, null, true)
                    .thenApplyAsync(voidvalue -> DummyContext.currentValue(), contextUnawareThreadpool);
            assertThat(future.get()).isEqualTo("Zed");
        }
    }

    @Test
    void testThenApplyAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Jimmie")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Bonnie")).takeNewSnapshot().thenApply(voidvalue -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).thenAccept(s -> {
                assertThat(s).isEqualTo("Jimmie");
                assertContext("-Jimmie");
            }).get();
        }

        try (Context ignored = manager.activate("Jimmie")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Bonnie"), null, null, true).thenApply(voidvalue -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).thenAccept(s -> {
                assertThat(s).isEqualTo("Bonnie");
                assertContext("-Bonnie");
            }).get();
        }
    }

    @Test
    void testThenApplyAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Butch")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Esmerelda Villalobos")).takeNewSnapshot().thenApplyAsync(voidvalue -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).thenAccept(s -> {
                assertThat(s).isEqualTo("Butch");
                assertContext("-Butch");
            }).get();
        }

        try (Context ignored = manager.activate("Butch")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Esmerelda Villalobos"), null, null, true).thenApplyAsync(voidvalue -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).thenAccept(s -> {
                assertThat(s).isEqualTo("Esmerelda Villalobos");
                assertContext("-Esmerelda Villalobos");
            }).get();
        }
    }

    @Test
    void testThenApplyAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Maynard")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Zed")).takeNewSnapshot().thenApplyAsync(voidvalue -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }, contextUnawareThreadpool).thenAccept(s -> {
                assertThat(s).isEqualTo("Maynard");
                assertContext("-Maynard");
            }).get();
        }

        try (Context ignored = manager.activate("Maynard")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Zed"), null, null, true).thenApplyAsync(voidvalue -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }, contextUnawareThreadpool).thenAccept(s -> {
                assertThat(s).isEqualTo("Zed");
                assertContext("-Zed");
            }).get();
        }
    }

    @Test
    void testThenAccept() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("The Gimp")) {
            Future<Void> future = ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Butch"))
                    .thenAccept(voidvalue -> assertContext("The Gimp"));
            future.get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("The Gimp")) {
            Future<Void> future = ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Butch"), null, null, true)
                    .thenAccept(voidvalue -> assertContext("Butch"));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenAcceptAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Butch")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Fabienne"))
                    .thenAcceptAsync(voidvalue -> assertContext("Butch"));
            future.get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Butch")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Fabienne"), null, null, true)
                    .thenAcceptAsync(voidvalue -> assertContext("Fabienne"));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenAcceptAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Marvin")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Winston Wolfe"))
                    .thenAcceptAsync(voidvalue -> assertContext("Marvin"), contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Marvin")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Winston Wolfe"), null, null, true)
                    .thenAcceptAsync(voidvalue -> assertContext("Winston Wolfe"), contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenAcceptAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("The Gimp")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Butch")).takeNewSnapshot().thenAccept(voidvalue -> {
                        String val = DummyContext.currentValue();
                        assertThat(val).isEqualTo("The Gimp");
                        DummyContext.setCurrentValue("-" + val);
                    }).thenAccept(aVoid -> assertContext("-The Gimp"))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("The Gimp")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Butch"), null, null, true).thenAccept(voidvalue -> {
                        String val = DummyContext.currentValue();
                        assertThat(val).isEqualTo("Butch");
                        DummyContext.setCurrentValue("-" + val);
                    }).thenAccept(aVoid -> assertContext("-Butch"))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenAcceptAsynAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Butch")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Fabienne")).takeNewSnapshot().thenAcceptAsync(voidvalue -> {
                        String val = DummyContext.currentValue();
                        assertThat(val).isEqualTo("Butch");
                        DummyContext.setCurrentValue("-" + val);
                    }).thenAccept(aVoid -> assertContext("-Butch"))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Butch")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Fabienne"), null, null, true).thenAcceptAsync(voidvalue -> {
                        String val = DummyContext.currentValue();
                        assertThat(val).isEqualTo("Fabienne");
                        DummyContext.setCurrentValue("-" + val);
                    }).thenAccept(aVoid -> assertContext("-Fabienne"))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenAcceptAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Marvin")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Winston Wolfe")).takeNewSnapshot().thenAcceptAsync(voidvalue -> {
                        String val = DummyContext.currentValue();
                        assertThat(val).isEqualTo("Marvin");
                        DummyContext.setCurrentValue("-" + val);
                    }, contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertContext("-Marvin"))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Marvin")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Winston Wolfe"), null, null, true).thenAcceptAsync(voidvalue -> {
                        String val = DummyContext.currentValue();
                        assertThat(val).isEqualTo("Winston Wolfe");
                        DummyContext.setCurrentValue("-" + val);
                    }, contextUnawareThreadpool).thenAccept(aVoid -> assertContext("-Winston Wolfe"))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenRun() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Lance")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Jody"))
                    .thenRun(() -> assertContext("Lance"));
            future.get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Lance")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Jody"), null, null, true)
                    .thenRun(() -> assertContext("Jody"))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenRunAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Ringo")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Yolanda"))
                    .thenRunAsync(() -> assertContext("Ringo"))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Ringo")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Yolanda"), null, null, true)
                    .thenRunAsync(() -> assertContext("Yolanda"))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenRunAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Capt. Koons")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Butch"))
                    .thenRunAsync(() -> assertContext("Capt. Koons"), contextUnawareThreadpool)
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Capt. Koons")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Butch"), null, null, true)
                    .thenRunAsync(() -> assertContext("Butch"), contextUnawareThreadpool)
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenRunAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Lance")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Jody"))
                    .takeNewSnapshot()
                    .thenRun(() -> {
                        String val = DummyContext.currentValue();
                        assertThat(val).isEqualTo("Lance");
                        DummyContext.setCurrentValue("-" + val);
                    })
                    .thenAccept(aVoid -> assertContext("-Lance"))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Lance")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Jody"), null, null, true)
                    .thenRun(() -> {
                        String val = DummyContext.currentValue();
                        assertThat(val).isEqualTo("Jody");
                        DummyContext.setCurrentValue("-" + val);
                    })
                    .thenAccept(aVoid -> assertContext("-Jody"))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenRunAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Ringo")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Yolanda"))
                    .takeNewSnapshot()
                    .thenRunAsync(() -> {
                        String val = DummyContext.currentValue();
                        assertThat(val).isEqualTo("Ringo");
                        DummyContext.setCurrentValue("-" + val);
                    })
                    .thenAccept(aVoid -> assertContext("-Ringo"))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Ringo")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Yolanda"), null, null, true)
                    .thenRunAsync(() -> {
                        String val = DummyContext.currentValue();
                        assertThat(val).isEqualTo("Yolanda");
                        DummyContext.setCurrentValue("-" + val);
                    })
                    .thenAccept(aVoid -> assertContext("-Yolanda"))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenRunAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Capt. Koons")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Butch"))
                    .takeNewSnapshot()
                    .thenRunAsync(() -> {
                        String val = DummyContext.currentValue();
                        assertThat(val).isEqualTo("Capt. Koons");
                        DummyContext.setCurrentValue("-" + val);
                    }, contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertContext("-Capt. Koons"))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Capt. Koons")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Butch"), null, null, true)
                    .thenRunAsync(() -> {
                        String val = DummyContext.currentValue();
                        assertThat(val).isEqualTo("Butch");
                        DummyContext.setCurrentValue("-" + val);
                    }, contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertContext("-Butch"))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testWhenComplete() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Floyd"))
                    .whenComplete((voidValue, exception) -> assertContext("Butch"))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Floyd"), null, null, true)
                    .whenComplete((voidValue, exception) -> assertContext("Floyd"))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testWhenCompleteAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Zed")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Pipe hittin' niggers"))
                    .whenCompleteAsync((voidValue, exception) -> assertContext("Zed"))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Zed")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Pipe hittin' niggers"), null, null, true)
                    .whenCompleteAsync((voidValue, exception) -> assertContext("Pipe hittin' niggers"))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testWhenCompleteAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Floyd"))
                    .whenCompleteAsync((voidValue, exception) -> assertContext("Butch"), contextUnawareThreadpool)
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Floyd"), null, null, true)
                    .whenCompleteAsync((voidValue, exception) -> assertContext("Floyd"), contextUnawareThreadpool)
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testHandle() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context ignored = manager.activate("Jody")) {
            assertThat(
                    ContextAwareCompletableFuture.runAsync(() -> {
                        DummyContext.setCurrentValue("Trudy");
                        throw exception;
                    }).handle((voidValue, throwable) -> manager.getActiveContextValue()).get())
                    .isEqualTo("Jody");
        }

        try (Context ignored = manager.activate("Jody")) {
            assertThat(
                    ContextAwareCompletableFuture.runAsync(() -> {
                        DummyContext.setCurrentValue("Trudy");
                        throw exception;
                    }, null, null, true).handle((voidValue, throwable) -> manager.getActiveContextValue()).get())
                    .isEqualTo("Trudy");
        }
    }

    @Test
    void testHandleAsync() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context ignored = manager.activate("Jody")) {
            assertThat(
                    ContextAwareCompletableFuture.runAsync(() -> {
                        DummyContext.setCurrentValue("Trudy");
                        throw exception;
                    }).handleAsync((voidValue, throwable) -> manager.getActiveContextValue()).get())
                    .isEqualTo("Jody");
        }

        try (Context ignored = manager.activate("Jody")) {
            assertThat(
                    ContextAwareCompletableFuture.runAsync(() -> {
                        DummyContext.setCurrentValue("Trudy");
                        throw exception;
                    }, null, null, true).handleAsync((voidValue, throwable) -> manager.getActiveContextValue()).get())
                    .isEqualTo("Trudy");
        }
    }

    @Test
    void testHandleAsync_executor() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context ignored = manager.activate("Jody")) {
            assertThat(
                    ContextAwareCompletableFuture.runAsync(() -> {
                        DummyContext.setCurrentValue("Trudy");
                        throw exception;
                    }).handleAsync((voidValue, throwable) -> manager.getActiveContextValue(), contextUnawareThreadpool).get())
                    .isEqualTo("Jody");
        }

        try (Context ignored = manager.activate("Jody")) {
            assertThat(
                    ContextAwareCompletableFuture.runAsync(() -> {
                        DummyContext.setCurrentValue("Trudy");
                        throw exception;
                    }, null, null, true).handleAsync((voidValue, throwable) -> manager.getActiveContextValue(), contextUnawareThreadpool).get())
                    .isEqualTo("Trudy");
        }
    }

    @Test
    void testHandleAndTakeSnapshot() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context ignored = manager.activate("Jody")) {
            assertThat(
                    ContextAwareCompletableFuture.runAsync(() -> {
                                DummyContext.setCurrentValue("Trudy");
                                throw exception;
                            })
                            .takeNewSnapshot()
                            .handle((voidValue, throwable) -> {
                                String val = manager.getActiveContextValue();
                                DummyContext.setCurrentValue("-" + val);
                                return val;
                            })
                            .whenComplete((result, throwable) -> assertContext("-Jody"))
                            .get())
                    .isEqualTo("Jody");
        }

        try (Context ignored = manager.activate("Jody")) {
            assertThat(
                    ContextAwareCompletableFuture.runAsync(() -> {
                                DummyContext.setCurrentValue("Trudy");
                                throw exception;
                            }, null, null, true)
                            .handle((voidValue, throwable) -> {
                                String val = manager.getActiveContextValue();
                                DummyContext.setCurrentValue("-" + val);
                                return val;
                            })
                            .whenComplete((result, throwable) -> assertContext("-Trudy"))
                            .get())
                    .isEqualTo("Trudy");
        }
    }

    @Test
    void testHandleAsyncAndTakeSnapshot() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context ignored = manager.activate("Jody")) {
            assertThat(
                    ContextAwareCompletableFuture.runAsync(() -> {
                                DummyContext.setCurrentValue("Trudy");
                                throw exception;
                            })
                            .takeNewSnapshot()
                            .handleAsync((voidValue, throwable) -> {
                                String val = manager.getActiveContextValue();
                                DummyContext.setCurrentValue("-" + val);
                                return val;
                            })
                            .whenComplete((result, throwable) -> assertContext("-Jody")).get())
                    .isEqualTo("Jody");
        }

        try (Context ignored = manager.activate("Jody")) {
            assertThat(
                    ContextAwareCompletableFuture.runAsync(() -> {
                                DummyContext.setCurrentValue("Trudy");
                                throw exception;
                            }, null, null, true)
                            .handleAsync((voidValue, throwable) -> {
                                String val = manager.getActiveContextValue();
                                DummyContext.setCurrentValue("-" + val);
                                return val;
                            })
                            .whenComplete((result, throwable) -> assertContext("-Trudy")).get())
                    .isEqualTo("Trudy");
        }
    }

    @Test
    void testHandleAsyncAndTakeSnapshot_executor() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context ignored = manager.activate("Jody")) {
            assertThat(
                    ContextAwareCompletableFuture.runAsync(() -> {
                                DummyContext.setCurrentValue("Trudy");
                                throw exception;
                            })
                            .takeNewSnapshot()
                            .handleAsync((voidValue, throwable) -> {
                                String val = manager.getActiveContextValue();
                                DummyContext.setCurrentValue("-" + val);
                                return val;
                            }, contextUnawareThreadpool)
                            .whenComplete((result, throwable) -> assertContext("-Jody")).get())
                    .isEqualTo("Jody");
        }

        try (Context ignored = manager.activate("Jody")) {
            assertThat(
                    ContextAwareCompletableFuture.runAsync(() -> {
                                DummyContext.setCurrentValue("Trudy");
                                throw exception;
                            }, null, null, true)
                            .handleAsync((voidValue, throwable) -> {
                                String val = manager.getActiveContextValue();
                                DummyContext.setCurrentValue("-" + val);
                                return val;
                            }, contextUnawareThreadpool)
                            .whenComplete((result, throwable) -> assertContext("-Trudy"))
                            .get())
                    .isEqualTo("Trudy");
        }
    }

    @Test
    void testExceptionally() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Gringo")) {
            ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Jules Winnfield");
                throw new RuntimeException("Bad Motherfucker");
            }).exceptionally(ex -> {
                assertContext("Gringo");
                return null;
            }).get();
        }

        try (Context ignored = manager.activate("Gringo")) {
            ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Jules Winnfield");
                throw new RuntimeException("Bad Motherfucker");
            }, null, null, true).exceptionally(ex -> {
                assertContext("Jules Winnfield");
                return null;
            }).get();
        }
    }

    @Test
    void testExceptionallyAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Gringo")) {
            ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Jules Winnfield");
                throw new RuntimeException("Bad Motherfucker");
            }).takeNewSnapshot().exceptionally(ex -> {
                String val = manager.getActiveContextValue();
                assertThat(val).isEqualTo("Gringo");
                DummyContext.setCurrentValue("-" + val);
                return null;
            }).thenAccept(aVoid -> assertContext("-Gringo")).get();
        }

        try (Context ignored = manager.activate("Gringo")) {
            ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Jules Winnfield");
                throw new RuntimeException("Bad Motherfucker");
            }, null, null, true).exceptionally(ex -> {
                String val = manager.getActiveContextValue();
                assertThat(val).isEqualTo("Jules Winnfield");
                DummyContext.setCurrentValue("-" + val);
                return null;
            }).thenAccept(aVoid -> assertContext("-Jules Winnfield")).get();
        }
    }

    @Test
    void testThenCombine() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Marcellus Wallace")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Vincent Vega"))
                    .thenCombine(ContextAwareCompletableFuture
                            .runAsync(() -> DummyContext.setCurrentValue("Jules Winnfield")), (voidA, voidB) -> DummyContext.currentValue());
            assertThat(future.get()).isEqualTo("Marcellus Wallace");
            assertContext("Marcellus Wallace");
        }

        try (Context ignored = manager.activate("Marcellus Wallace")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Vincent Vega"), null, null, true)
                    .thenCombine(ContextAwareCompletableFuture
                            .runAsync(() -> DummyContext.setCurrentValue("Jules Winnfield")), (voidA, voidB) -> DummyContext.currentValue());
            assertThat(future.get()).isEqualTo("Vincent Vega");
            assertContext("Marcellus Wallace");
        }
    }

    @Test
    void testThenCombineAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Marvin"))
                    .thenCombineAsync(ContextAwareCompletableFuture
                            .runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> DummyContext.currentValue());
            assertThat(future.get()).isEqualTo("Brett");
            assertContext("Brett");
        }

        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Marvin"), null, null, true)
                    .thenCombineAsync(ContextAwareCompletableFuture
                            .runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> DummyContext.currentValue());
            assertThat(future.get()).isEqualTo("Marvin");
            assertContext("Brett");
        }
    }

    @Test
    void testThenCombineAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Marvin"))
                    .thenCombineAsync(ContextAwareCompletableFuture
                            .runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> DummyContext.currentValue(), contextUnawareThreadpool);
            assertThat(future.get()).isEqualTo("Brett");
            assertContext("Brett");
        }

        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Marvin"), null, null, true)
                    .thenCombineAsync(ContextAwareCompletableFuture
                            .runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> DummyContext.currentValue(), contextUnawareThreadpool);
            assertThat(future.get()).isEqualTo("Marvin");
            assertContext("Brett");
        }
    }

    @Test
    void testThenCombineAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Marcellus Wallace")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Vincent Vega"))
                    .takeNewSnapshot()
                    .thenCombine(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Jules Winnfield")), (voidA, voidB) -> {
                        String val = DummyContext.currentValue();
                        DummyContext.setCurrentValue("-" + val);
                        return val;
                    }).whenComplete((result, exception) -> assertContext("-Marcellus Wallace"));

            assertThat(future.get()).isEqualTo("Marcellus Wallace");
        }

        try (Context ignored = manager.activate("Marcellus Wallace")) {
            Future<String> future = ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Vincent Vega"), null, null, true).thenCombine(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Jules Winnfield")), (voidA, voidB) -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).whenComplete((result, exception) -> assertContext("-Vincent Vega"));

            assertThat(future.get()).isEqualTo("Vincent Vega");
        }
    }

    @Test
    void testThenCombineAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Marvin")).takeNewSnapshot().thenCombineAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).whenComplete((result, exeption) -> assertContext("-Brett"));

            assertThat(future.get()).isEqualTo("Brett");
            assertContext("Brett");
        }

        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Marvin"), null, null, true).thenCombineAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).whenComplete((result, exeption) -> assertContext("-Marvin"));

            assertThat(future.get()).isEqualTo("Marvin");
            assertContext("Brett");
        }
    }

    @Test
    void testThenCombineAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Marvin")).takeNewSnapshot().thenCombineAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }, contextUnawareThreadpool).whenComplete((result, exeption) -> assertContext("-Brett"));

            assertThat(future.get()).isEqualTo("Brett");
            assertContext("Brett");
        }

        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Marvin"), null, null, true).thenCombineAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }, contextUnawareThreadpool).whenComplete((result, exeption) -> assertContext("-Marvin"));

            assertThat(future.get()).isEqualTo("Marvin");
            assertContext("Brett");
        }
    }

    @Test
    void testThenAcceptBoth() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Quentin"), null, null, true)
                    .thenAcceptBoth(completedFuture("Tarantino"), (Void voidA, String stringB) -> assertThat(manager.getActiveContextValue() + stringB).isEqualTo("QuentinTarantino"))
                    .get();
            assertContext("Pulp Fiction");
        }
    }

    @Test
    void testThenAcceptBothAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Quentin"), null, null, true)
                    .thenAcceptBothAsync(completedFuture("Tarantino"), (Void voidA, String stringB) -> assertThat(manager.getActiveContextValue() + stringB).isEqualTo("QuentinTarantino"))
                    .get();
            assertContext("Pulp Fiction");
        }
    }

    @Test
    void testThenAcceptBothAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Quentin"), null, null, true)
                    .thenAcceptBothAsync(completedFuture("Tarantino"), (Void voidA, String stringB) -> assertThat(manager.getActiveContextValue() + stringB).isEqualTo("QuentinTarantino"), contextUnawareThreadpool)
                    .get();
            assertContext("Pulp Fiction");
        }
    }

    @Test
    void testThenAcceptBothAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Quentin"))
                    .takeNewSnapshot()
                    .thenAcceptBoth(completedFuture(" by Tarantino"), (Void voidA, String stringB) -> {
                        String val = manager.getActiveContextValue() + stringB;
                        assertThat(val).isEqualTo("Pulp Fiction by Tarantino");
                        DummyContext.setCurrentValue("-" + val);
                    })
                    .thenAccept(aVoid -> assertContext("-Pulp Fiction by Tarantino"))
                    .get();
        }

        try (Context ignored = manager.activate("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Quentin"), null, null, true)
                    .thenAcceptBoth(completedFuture(" by Tarantino"), (Void voidA, String stringB) -> {
                        String val = manager.getActiveContextValue() + stringB;
                        assertThat(val).isEqualTo("Quentin by Tarantino");
                        DummyContext.setCurrentValue("-" + val);
                    })
                    .thenAccept(aVoid -> assertContext("-Quentin by Tarantino"))
                    .get();
        }
    }

    @Test
    void testThenAcceptBothAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Quentin"))
                    .takeNewSnapshot()
                    .thenAcceptBoth(completedFuture(" by Tarantino"), (Void voidA, String stringB) -> {
                        String val = manager.getActiveContextValue() + stringB;
                        assertThat(val).isEqualTo("Pulp Fiction by Tarantino");
                        DummyContext.setCurrentValue("-" + val);
                    })
                    .thenAccept(aVoid -> assertContext("-Pulp Fiction by Tarantino"))
                    .get();
        }

        try (Context ignored = manager.activate("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Quentin"), null, null, true)
                    .thenAcceptBoth(completedFuture("Tarantino"), (Void voidA, String stringB) -> {
                        String val = manager.getActiveContextValue() + stringB;
                        assertThat(val).isEqualTo("QuentinTarantino");
                        DummyContext.setCurrentValue("-" + val);
                    })
                    .thenAccept(aVoid -> assertContext("-QuentinTarantino"))
                    .get();
        }
    }

    @Test
    void testThenAcceptBothAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Quentin"))
                    .takeNewSnapshot()
                    .thenAcceptBothAsync(completedFuture(" by Tarantino"), (Void voidA, String stringB) -> {
                        String val = manager.getActiveContextValue() + stringB;
                        assertThat(val).isEqualTo("Pulp Fiction by Tarantino");
                        DummyContext.setCurrentValue("-" + val);
                    }, contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertContext("-Pulp Fiction by Tarantino"))
                    .get();
            assertContext("Pulp Fiction");
        }

        try (Context ignored = manager.activate("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Quentin"), null, null, true)
                    .thenAcceptBothAsync(completedFuture("Tarantino"), (Void voidA, String stringB) -> {
                        String val = manager.getActiveContextValue() + stringB;
                        assertThat(val).isEqualTo("QuentinTarantino");
                        DummyContext.setCurrentValue("-" + val);
                    }, contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertContext("-QuentinTarantino"))
                    .get();
            assertContext("Pulp Fiction");
        }
    }

    @Test
    void testRunAfterBoth() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Ketchup"))
                    .runAfterBoth(
                            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")),
                            () -> assertContext("French Fries"))
                    .get();
        }

        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Ketchup"), null, null, true)
                    .runAfterBoth(
                            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")),
                            () -> assertContext("Ketchup"))
                    .get();
        }
    }

    @Test
    void testRunAfterBothAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Ketchup"))
                    .runAfterBothAsync(
                            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")),
                            () -> assertContext("French Fries"))
                    .get();
        }

        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Ketchup"), null, null, true)
                    .runAfterBothAsync(
                            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")),
                            () -> assertContext("Ketchup"))
                    .get();
        }
    }

    @Test
    void testRunAfterBothAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Ketchup"))
                    .runAfterBothAsync(
                            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")),
                            () -> assertContext("French Fries"),
                            contextUnawareThreadpool)
                    .get();
        }

        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Ketchup"), null, null, true)
                    .runAfterBothAsync(
                            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")),
                            () -> assertContext("Ketchup"),
                            contextUnawareThreadpool)
                    .get();
        }
    }

    @Test
    void testRunAfterBothAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup")).takeNewSnapshot().runAfterBoth(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> {
                String val = manager.getActiveContextValue();
                assertThat(val).isEqualTo("French Fries");
                DummyContext.setCurrentValue("-" + val);
            }).thenAccept(aVoid -> assertContext("-French Fries")).get();
        }

        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup"), null, null, true).runAfterBoth(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> {
                String val = manager.getActiveContextValue();
                assertThat(val).isEqualTo("Ketchup");
                DummyContext.setCurrentValue("-" + val);
            }).thenAccept(aVoid -> assertContext("-Ketchup")).get();
        }
    }

    @Test
    void testRunAfterBothAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup")).takeNewSnapshot().runAfterBothAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> {
                String val = manager.getActiveContextValue();
                assertThat(val).isEqualTo("French Fries");
                DummyContext.setCurrentValue("-" + val);
            }).thenAccept(aVoid -> assertContext("-French Fries")).get();
        }

        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup"), null, null, true).runAfterBothAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> {
                String val = manager.getActiveContextValue();
                assertThat(val).isEqualTo("Ketchup");
                DummyContext.setCurrentValue("-" + val);
            }).thenAccept(aVoid -> assertContext("-Ketchup")).get();
        }
    }

    @Test
    void testRunAfterBothAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup")).takeNewSnapshot().runAfterBothAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> {
                String val = manager.getActiveContextValue();
                assertThat(val).isEqualTo("French Fries");
                DummyContext.setCurrentValue("-" + val);
            }, contextUnawareThreadpool).thenAccept(aVoid -> assertContext("-French Fries")).get();
        }

        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup"), null, null, true).runAfterBothAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> {
                String val = manager.getActiveContextValue();
                assertThat(val).isEqualTo("Ketchup");
                DummyContext.setCurrentValue("-" + val);
            }, contextUnawareThreadpool).thenAccept(aVoid -> assertContext("-Ketchup")).get();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testApplyToEither(boolean takeNewSnapshot) throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<String> result = future1.takeNewSnapshot(takeNewSnapshot).applyToEither(future2, input -> String.format("Winner: %s, Thread: %s, Context: %s", input, Thread.currentThread().getName(), manager.getActiveContextValue()));

            // verify
            assertThat(result.get())
                    .startsWith("Winner: Function")
                    .endsWith("Context: Parent");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testApplyToEitherAsync(boolean takeNewSnapshot) throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<String> result = future1.takeNewSnapshot(takeNewSnapshot).applyToEitherAsync(future2, input -> String.format("Winner: %s, Thread: %s, Context: %s", input, Thread.currentThread().getName(), manager.getActiveContextValue()));

            // verify
            assertThat(result.get())
                    .startsWith("Winner: Function")
                    .endsWith("Context: Parent");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testApplyToEitherAsync_executor(boolean takeNewSnapshot) throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<String> result = future1.takeNewSnapshot(takeNewSnapshot).applyToEitherAsync(future2, input -> String.format("Winner: %s, Thread: %s, Context: %s", input, Thread.currentThread().getName(), manager.getActiveContextValue()), contextUnawareThreadpool);

            // verify
            assertThat(result.get())
                    .startsWith("Winner: Function")
                    .endsWith("Context: Parent");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAcceptEither(boolean takeNewSnapshot) {
        try (Context ignored = manager.activate("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<Void> result = future1.takeNewSnapshot(takeNewSnapshot).acceptEither(future2, input -> {
                assertThat(input).startsWith("Function");
                assertContext("Parent");
            });

            // verify
            assertDoesNotThrow(get(result));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAcceptEitherAsync(boolean takeNewSnapshot) {
        try (Context ignored = manager.activate("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<Void> result = future1.takeNewSnapshot(takeNewSnapshot).acceptEitherAsync(future2, input -> {
                assertThat(input).startsWith("Function");
                assertContext("Parent");
            });

            // verify
            assertDoesNotThrow(get(result));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAcceptEitherAsync_executor(boolean takeNewSnapshot) {
        try (Context ignored = manager.activate("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<Void> result = future1.takeNewSnapshot(takeNewSnapshot).acceptEitherAsync(future2, input -> {
                assertThat(input).startsWith("Function");
                assertContext("Parent");
            }, contextUnawareThreadpool);

            // verify
            assertDoesNotThrow(get(result));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testRunAfterEither(boolean takeNewSnapshot) {
        try (Context ignored = manager.activate("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<Void> result = future1.takeNewSnapshot(takeNewSnapshot).runAfterEither(future2, () -> assertContext("Parent"));

            // verify
            assertDoesNotThrow(get(result));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testRunAfterEitherAsync(boolean takeNewSnapshot) {
        try (Context ignored = manager.activate("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<Void> result = future1.takeNewSnapshot(takeNewSnapshot).runAfterEitherAsync(future2, () -> assertContext("Parent"));

            // verify
            assertDoesNotThrow(get(result));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testRunAfterEitherAsync_executor(boolean takeNewSnapshot) {
        try (Context ignored = manager.activate("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<Void> result = future1.takeNewSnapshot(takeNewSnapshot).runAfterEitherAsync(future2, () -> assertContext("Parent"), contextUnawareThreadpool);

            // verify
            assertDoesNotThrow(get(result));
        }
    }

    @Test
    void testThenCompose() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("John")) {
            assertThat(
                    supplyAsync(() -> {
                        String current = manager.getActiveContextValue();
                        DummyContext.setCurrentValue("Travolta");
                        return current;
                    })
                            .thenCompose(value -> supplyAsync(() -> value + manager.getActiveContextValue()))
                            .get()
            ).isEqualTo("JohnJohn");
        }

        try (Context ignored = manager.activate("John")) {
            assertThat(
                    supplyAsync(() -> {
                        String current = manager.getActiveContextValue();
                        DummyContext.setCurrentValue("Travolta");
                        return current;
                    }, null, null, true)
                            .thenCompose(value -> supplyAsync(() -> value + manager.getActiveContextValue()))
                            .get()
            ).isEqualTo("JohnTravolta");
        }
    }

    @Test
    void testThenComposeAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("John")) {
            assertThat(
                    supplyAsync(() -> {
                        String current = manager.getActiveContextValue();
                        DummyContext.setCurrentValue("Travolta");
                        return current;
                    })
                            .thenComposeAsync(value -> supplyAsync(() -> value + manager.getActiveContextValue()))
                            .get()
            ).isEqualTo("JohnJohn");
        }

        try (Context ignored = manager.activate("John")) {
            assertThat(
                    supplyAsync(() -> {
                        String current = manager.getActiveContextValue();
                        DummyContext.setCurrentValue("Travolta");
                        return current;
                    }, null, null, true)
                            .thenComposeAsync(value -> supplyAsync(() -> value + manager.getActiveContextValue()))
                            .get()
            ).isEqualTo("JohnTravolta");
        }
    }

    @Test
    void testThenComposeAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("John")) {
            assertThat(
                    supplyAsync(() -> {
                        String current = manager.getActiveContextValue();
                        DummyContext.setCurrentValue("Travolta");
                        return current;
                    })
                            .thenComposeAsync(value -> completedFuture(value + manager.getActiveContextValue()), contextUnawareThreadpool)
                            .get()
            ).isEqualTo("JohnJohn");
        }

        try (Context ignored = manager.activate("John")) {
            assertThat(
                    supplyAsync(() -> {
                        String current = manager.getActiveContextValue();
                        DummyContext.setCurrentValue("Travolta");
                        return current;
                    }, null, null, true)
                            .thenComposeAsync(value -> completedFuture(value + manager.getActiveContextValue()), contextUnawareThreadpool)
                            .get()
            ).isEqualTo("JohnTravolta");
        }
    }

    @Test
    void testTimingIssue55() throws ExecutionException, InterruptedException, TimeoutException {
        try (Context ignored = manager.activate("Vincent Vega")) {
            final CountDownLatch latch1 = new CountDownLatch(1), latch2 = new CountDownLatch(1);
            ContextAwareCompletableFuture<String> future1 = supplyAsync(() -> {
                String result = Optional.ofNullable(DummyContext.currentValue()).orElse("NO VALUE");
                DummyContext.setCurrentValue("Jules Winnfield");
                waitFor(latch1);
                return result;
            }, null, null, true);
            ContextAwareCompletableFuture<String> future2 = future1.thenApplyAsync(value -> {
                String result = value + ", " + Optional.ofNullable(DummyContext.currentValue()).orElse("NO VALUE");
                DummyContext.setCurrentValue("Marcellus Wallace");
                waitFor(latch2);
                return result;
            });
            Future<String> future3 = future2.thenApplyAsync(value -> value + ", " + Optional.ofNullable(DummyContext.currentValue()).orElse("NO VALUE"));

            assertThat(future1.isDone()).as("Future creation may not block on previous stages").isFalse();
            assertThat(future2.isDone()).as("Future creation may not block on previous stages").isFalse();
            assertThat(future3.isDone()).as("Future creation may not block on previous stages").isFalse();

            latch1.countDown();
            future1.get(500, TimeUnit.MILLISECONDS);
            assertThat(future1.isDone()).isTrue();
            assertThat(future2.isDone()).isFalse();
            assertThat(future3.isDone()).isFalse();

            latch2.countDown();
            future2.get(500, TimeUnit.MILLISECONDS);
            assertThat(future2.isDone()).isTrue();
            assertThat(future3.get(500, TimeUnit.MILLISECONDS))
                    .isEqualTo("Vincent Vega, Jules Winnfield, Marcellus Wallace");
            assertContext("Vincent Vega");
        }
    }

    @Test
    void testAllOf() throws ExecutionException, InterruptedException {
        DummyContext.setCurrentValue("Vincent Vega");
        CompletableFuture<String> cf1 = new CompletableFuture<>();
        CompletableFuture<String> cf2 = new ContextAwareCompletableFuture<String>().takeNewSnapshot().thenApply(s -> {
            DummyContext.setCurrentValue("-" + s); // This context should be ignored
            return s;
        });
        ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.allOf(cf1, cf2);
        DummyContext.setCurrentValue("Jules Winnfield");

        ContextAwareCompletableFuture<String> result = future.thenApplyAsync(aVoid -> manager.getActiveContextValue());
        assertThat(result.isDone()).isFalse();
        cf1.complete("Value 1");
        assertThat(result.isDone()).isFalse();
        cf2.complete("Value 2");
        assertThat(result.get()).isEqualTo("Vincent Vega");
    }

    @Test
    void testAllOfWithSpecificSnapshot() throws ExecutionException, InterruptedException {
        DummyContext.setCurrentValue("Vincent Vega");
        final ContextSnapshot snapshot = ContextSnapshot.capture();
        DummyContext.setCurrentValue("Marcellus Wallace");
        CompletableFuture<String> cf1 = new CompletableFuture<>();
        CompletableFuture<String> cf2 = new ContextAwareCompletableFuture<String>().takeNewSnapshot().thenApply(s -> {
            DummyContext.setCurrentValue("-" + s); // This context should be ignored
            return s;
        });
        ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.allOf(snapshot, cf1, cf2);
        DummyContext.setCurrentValue("Jules Winnfield");

        ContextAwareCompletableFuture<String> result = future.thenApplyAsync(aVoid -> manager.getActiveContextValue());
        assertThat(result.isDone()).isFalse();
        cf1.complete("Value 1");
        assertThat(result.isDone()).isFalse();
        cf2.complete("Value 2");
        assertThat(result.get()).isEqualTo("Vincent Vega");
    }

    @Test
    void testAnyOf() throws ExecutionException, InterruptedException {
        DummyContext.setCurrentValue("Vincent Vega");
        CompletableFuture<String> cf1 = new CompletableFuture<>();
        CompletableFuture<String> cf2 = new ContextAwareCompletableFuture<String>().takeNewSnapshot().thenApply(s -> {
            DummyContext.setCurrentValue("-" + s); // This context should be ignored
            return s;
        });
        ContextAwareCompletableFuture<Object> future = ContextAwareCompletableFuture.anyOf(cf1, cf2);
        DummyContext.setCurrentValue("Jules Winnfield");

        ContextAwareCompletableFuture<String> result = future.thenApplyAsync(s -> manager.getActiveContextValue());
        assertThat(result.isDone()).isFalse();
        cf2.complete("Value 2");
        assertThat(result.get()).isEqualTo("Vincent Vega");
        assertThat(future.get()).isEqualTo("Value 2");
    }

    @Test
    void testAnyOfWithSpecificSnapshot() throws ExecutionException, InterruptedException {
        DummyContext.setCurrentValue("Vincent Vega");
        final ContextSnapshot snapshot = ContextSnapshot.capture();
        DummyContext.setCurrentValue("Marcellus Wallace");
        CompletableFuture<String> cf1 = new CompletableFuture<>();
        CompletableFuture<String> cf2 = new ContextAwareCompletableFuture<String>().takeNewSnapshot().thenApply(s -> {
            DummyContext.setCurrentValue("-" + s); // This context should be ignored
            return s;
        });
        ContextAwareCompletableFuture<Object> future = ContextAwareCompletableFuture.anyOf(snapshot, cf1, cf2);
        DummyContext.setCurrentValue("Jules Winnfield");

        ContextAwareCompletableFuture<String> result = future.thenApplyAsync(s -> manager.getActiveContextValue());
        assertThat(result.isDone()).isFalse();
        cf1.complete("Value 1");
        assertThat(result.get()).isEqualTo("Vincent Vega");
        assertThat(future.get()).isEqualTo("Value 1");
    }

    static void waitFor(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted waiting for latch.", ie);
        }
    }

    static <T> ThrowingSupplier<T> get(Future<T> future) {
        return future::get;
    }
}
