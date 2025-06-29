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
package nl.talsmasoftware.context.core.concurrent;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.dummy.DummyContext;
import nl.talsmasoftware.context.dummy.DummyContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
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

    static void assertContext(String expectedValue) {
        assertThat("Active context value", manager.getActiveContextValue(), is(expectedValue));
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
        assertThat(manager.getActiveContextValue(), is(nullValue()));
        assertThat(future1.isDone(), is(false));
        assertThat(future2.isDone(), is(false));

        assertThat(future1.complete("bar"), is(true));
        assertThat(future1.isDone(), is(true));
        assertThat(future2.isDone(), is(true));
        assertThat(future2.get(), is("foobar"));
    }

    @Test
    void testSupplyAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Vincent Vega")) {
            Future<String> future = supplyAsync(DummyContext::currentValue);
            assertThat(future.get(), is("Vincent Vega"));
        }
    }

    @Test
    void testSupplyAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Marcellus Wallace")) {
            Future<String> future = supplyAsync(DummyContext::currentValue, contextUnawareThreadpool);
            assertThat(future.get(), is("Marcellus Wallace"));
        }
    }

    @Test
    void testSupplyAsync_executor_snapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Vincent Vega")) {
            ContextSnapshot snapshot = ContextSnapshot.capture();
            DummyContext.setCurrentValue("Jules Winnfield");
            assertContext("Jules Winnfield");

            Future<String> future = supplyAsync(DummyContext::currentValue, contextUnawareThreadpool, snapshot);
            assertThat(future.get(), is("Vincent Vega"));
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

            assertThat(future.get(), is("Vincent Vega"));
            assertThat(future.thenApply(x -> DummyContext.currentValue()).get(), is("Marcellus Wallace"));
        }
    }

    @Test
    void testRunAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Mia Wallace")) {
            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(() -> assertThat(DummyContext.currentValue(), is("Mia Wallace")));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testRunAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Jimmie")) {
            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(() -> assertThat(DummyContext.currentValue(), is("Jimmie")), contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testRunAsync_executor_snapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Pumpkin")) {
            ContextSnapshot snapshot = ContextSnapshot.capture();
            DummyContext.setCurrentValue("Honey Bunny");
            assertContext("Honey Bunny");

            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(() -> assertThat(DummyContext.currentValue(), is("Pumpkin")), contextUnawareThreadpool, snapshot);
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
                assertThat(DummyContext.currentValue(), is("Pumpkin"));
                DummyContext.setCurrentValue("Bad Motherfucker");
            }, contextUnawareThreadpool, snapshot, true);
            future.get(); // trigger asynchronous assertion and makes sure the function already ended.
            future.thenRun(() -> assertThat(DummyContext.currentValue(), is("Bad Motherfucker"))).get();
        }
    }

    @Test
    void testThenApply() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Jimmie")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Bonnie"))
                    .thenApply(voidvalue -> DummyContext.currentValue());
            assertThat(future.get(), is("Jimmie"));
        }

        try (Context ignored = manager.activate("Jimmie")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Bonnie"), null, null, true)
                    .thenApply(voidvalue -> DummyContext.currentValue());
            assertThat(future.get(), is("Bonnie"));
        }
    }

    @Test
    void testThenApplyAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Butch")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Esmerelda Villalobos"))
                    .thenApplyAsync(voidvalue -> DummyContext.currentValue());
            assertThat(future.get(), is("Butch"));
        }

        try (Context ignored = manager.activate("Butch")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Esmerelda Villalobos"), null, null, true)
                    .thenApplyAsync(voidvalue -> DummyContext.currentValue());
            assertThat(future.get(), is("Esmerelda Villalobos"));
        }
    }

    @Test
    void testThenApplyAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Maynard")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Zed"))
                    .thenApplyAsync(voidvalue -> DummyContext.currentValue(), contextUnawareThreadpool);
            assertThat(future.get(), is("Maynard"));
        }

        try (Context ignored = manager.activate("Maynard")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Zed"), null, null, true)
                    .thenApplyAsync(voidvalue -> DummyContext.currentValue(), contextUnawareThreadpool);
            assertThat(future.get(), is("Zed"));
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
                assertThat(s, is("Jimmie"));
                assertThat(DummyContext.currentValue(), is("-Jimmie"));
            }).get();
        }

        try (Context ignored = manager.activate("Jimmie")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Bonnie"), null, null, true).thenApply(voidvalue -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).thenAccept(s -> {
                assertThat(s, is("Bonnie"));
                assertThat(DummyContext.currentValue(), is("-Bonnie"));
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
                assertThat(s, is("Butch"));
                assertThat(DummyContext.currentValue(), is("-Butch"));
            }).get();
        }

        try (Context ignored = manager.activate("Butch")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Esmerelda Villalobos"), null, null, true).thenApplyAsync(voidvalue -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).thenAccept(s -> {
                assertThat(s, is("Esmerelda Villalobos"));
                assertThat(DummyContext.currentValue(), is("-Esmerelda Villalobos"));
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
                assertThat(s, is("Maynard"));
                assertThat(DummyContext.currentValue(), is("-Maynard"));
            }).get();
        }

        try (Context ignored = manager.activate("Maynard")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Zed"), null, null, true).thenApplyAsync(voidvalue -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }, contextUnawareThreadpool).thenAccept(s -> {
                assertThat(s, is("Zed"));
                assertThat(DummyContext.currentValue(), is("-Zed"));
            }).get();
        }
    }

    @Test
    void testThenAccept() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("The Gimp")) {
            Future<Void> future = ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Butch"))
                    .thenAccept(voidvalue -> assertThat(DummyContext.currentValue(), is("The Gimp")));
            future.get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("The Gimp")) {
            Future<Void> future = ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Butch"), null, null, true)
                    .thenAccept(voidvalue -> assertThat(DummyContext.currentValue(), is("Butch")));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenAcceptAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Butch")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Fabienne"))
                    .thenAcceptAsync(voidvalue -> assertThat(DummyContext.currentValue(), is("Butch")));
            future.get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Butch")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Fabienne"), null, null, true)
                    .thenAcceptAsync(voidvalue -> assertThat(DummyContext.currentValue(), is("Fabienne")));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenAcceptAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Marvin")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Winston Wolfe"))
                    .thenAcceptAsync(voidvalue -> assertThat(DummyContext.currentValue(), is("Marvin")), contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Marvin")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Winston Wolfe"), null, null, true)
                    .thenAcceptAsync(voidvalue -> assertThat(DummyContext.currentValue(), is("Winston Wolfe")), contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenAcceptAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("The Gimp")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Butch")).takeNewSnapshot().thenAccept(voidvalue -> {
                        String val = DummyContext.currentValue();
                        assertThat(val, is("The Gimp"));
                        DummyContext.setCurrentValue("-" + val);
                    }).thenAccept(aVoid -> assertThat(DummyContext.currentValue(), is("-The Gimp")))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("The Gimp")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Butch"), null, null, true).thenAccept(voidvalue -> {
                        String val = DummyContext.currentValue();
                        assertThat(val, is("Butch"));
                        DummyContext.setCurrentValue("-" + val);
                    }).thenAccept(aVoid -> assertThat(DummyContext.currentValue(), is("-Butch")))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenAcceptAsynAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Butch")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Fabienne")).takeNewSnapshot().thenAcceptAsync(voidvalue -> {
                        String val = DummyContext.currentValue();
                        assertThat(val, is("Butch"));
                        DummyContext.setCurrentValue("-" + val);
                    }).thenAccept(aVoid -> assertThat(DummyContext.currentValue(), is("-Butch")))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Butch")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Fabienne"), null, null, true).thenAcceptAsync(voidvalue -> {
                        String val = DummyContext.currentValue();
                        assertThat(val, is("Fabienne"));
                        DummyContext.setCurrentValue("-" + val);
                    }).thenAccept(aVoid -> assertThat(DummyContext.currentValue(), is("-Fabienne")))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenAcceptAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Marvin")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Winston Wolfe")).takeNewSnapshot().thenAcceptAsync(voidvalue -> {
                        String val = DummyContext.currentValue();
                        assertThat(val, is("Marvin"));
                        DummyContext.setCurrentValue("-" + val);
                    }, contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertThat(DummyContext.currentValue(), is("-Marvin")))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Marvin")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Winston Wolfe"), null, null, true).thenAcceptAsync(voidvalue -> {
                        String val = DummyContext.currentValue();
                        assertThat(val, is("Winston Wolfe"));
                        DummyContext.setCurrentValue("-" + val);
                    }, contextUnawareThreadpool).thenAccept(aVoid -> assertThat(DummyContext.currentValue(), is("-Winston Wolfe")))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenRun() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Lance")) {
            Future<Void> future = ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Jody")).thenRun(() -> assertThat(DummyContext.currentValue(), is("Lance")));
            future.get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Lance")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Jody"), null, null, true)
                    .thenRun(() -> assertThat(DummyContext.currentValue(), is("Jody")))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenRunAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Ringo")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Yolanda"))
                    .thenRunAsync(() -> assertThat(DummyContext.currentValue(), is("Ringo")))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Ringo")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Yolanda"), null, null, true)
                    .thenRunAsync(() -> assertThat(DummyContext.currentValue(), is("Yolanda")))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testThenRunAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Capt. Koons")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Butch"))
                    .thenRunAsync(() -> assertThat(DummyContext.currentValue(), is("Capt. Koons")), contextUnawareThreadpool)
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Capt. Koons")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Butch"), null, null, true)
                    .thenRunAsync(() -> assertThat(DummyContext.currentValue(), is("Butch")), contextUnawareThreadpool)
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
                        assertThat(val, is("Lance"));
                        DummyContext.setCurrentValue("-" + val);
                    })
                    .thenAccept(aVoid -> assertThat(DummyContext.currentValue(), is("-Lance")))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Lance")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Jody"), null, null, true)
                    .thenRun(() -> {
                        String val = DummyContext.currentValue();
                        assertThat(val, is("Jody"));
                        DummyContext.setCurrentValue("-" + val);
                    })
                    .thenAccept(aVoid -> assertThat(DummyContext.currentValue(), is("-Jody")))
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
                        assertThat(val, is("Ringo"));
                        DummyContext.setCurrentValue("-" + val);
                    })
                    .thenAccept(aVoid -> assertThat(DummyContext.currentValue(), is("-Ringo")))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Ringo")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Yolanda"), null, null, true)
                    .thenRunAsync(() -> {
                        String val = DummyContext.currentValue();
                        assertThat(val, is("Yolanda"));
                        DummyContext.setCurrentValue("-" + val);
                    })
                    .thenAccept(aVoid -> assertThat(DummyContext.currentValue(), is("-Yolanda")))
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
                        assertThat(val, is("Capt. Koons"));
                        DummyContext.setCurrentValue("-" + val);
                    }, contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertThat(DummyContext.currentValue(), is("-Capt. Koons")))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Capt. Koons")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Butch"), null, null, true)
                    .thenRunAsync(() -> {
                        String val = DummyContext.currentValue();
                        assertThat(val, is("Butch"));
                        DummyContext.setCurrentValue("-" + val);
                    }, contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertThat(DummyContext.currentValue(), is("-Butch")))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testWhenComplete() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Floyd"))
                    .whenComplete((voidValue, exception) -> assertThat(DummyContext.currentValue(), is("Butch")))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Floyd"), null, null, true)
                    .whenComplete((voidValue, exception) -> assertThat(DummyContext.currentValue(), is("Floyd")))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testWhenCompleteAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Zed")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Pipe hittin' niggers"))
                    .whenCompleteAsync((voidValue, exception) -> assertThat(DummyContext.currentValue(), is("Zed")))
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Zed")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Pipe hittin' niggers"), null, null, true)
                    .whenCompleteAsync((voidValue, exception) -> assertThat(DummyContext.currentValue(), is("Pipe hittin' niggers")))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testWhenCompleteAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Floyd"))
                    .whenCompleteAsync((voidValue, exception) -> assertThat(DummyContext.currentValue(), is("Butch")), contextUnawareThreadpool)
                    .get(); // trigger asynchronous assertion
        }

        try (Context ignored = manager.activate("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Floyd"), null, null, true)
                    .whenCompleteAsync((voidValue, exception) -> assertThat(DummyContext.currentValue(), is("Floyd")), contextUnawareThreadpool)
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    void testHandle() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context ignored = manager.activate("Jody")) {
            assertThat(ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Trudy");
                throw exception;
            }).handle((voidValue, throwable) -> manager.getActiveContextValue()).get(), is("Jody"));
        }

        try (Context ignored = manager.activate("Jody")) {
            assertThat(ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Trudy");
                throw exception;
            }, null, null, true).handle((voidValue, throwable) -> manager.getActiveContextValue()).get(), is("Trudy"));
        }
    }

    @Test
    void testHandleAsync() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context ignored = manager.activate("Jody")) {
            assertThat(ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Trudy");
                throw exception;
            }).handleAsync((voidValue, throwable) -> manager.getActiveContextValue()).get(), is("Jody"));
        }

        try (Context ignored = manager.activate("Jody")) {
            assertThat(ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Trudy");
                throw exception;
            }, null, null, true).handleAsync((voidValue, throwable) -> manager.getActiveContextValue()).get(), is("Trudy"));
        }
    }

    @Test
    void testHandleAsync_executor() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context ignored = manager.activate("Jody")) {
            assertThat(ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Trudy");
                throw exception;
            }).handleAsync((voidValue, throwable) -> manager.getActiveContextValue(), contextUnawareThreadpool).get(), is("Jody"));
        }

        try (Context ignored = manager.activate("Jody")) {
            assertThat(ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Trudy");
                throw exception;
            }, null, null, true).handleAsync((voidValue, throwable) -> manager.getActiveContextValue(), contextUnawareThreadpool).get(), is("Trudy"));
        }
    }

    @Test
    void testHandleAndTakeSnapshot() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context ignored = manager.activate("Jody")) {
            assertThat(ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Trudy");
                throw exception;
            }).takeNewSnapshot().handle((voidValue, throwable) -> {
                String val = manager.getActiveContextValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).whenComplete((result, throwable) -> assertContext("-Jody")).get(), is("Jody"));
        }

        try (Context ignored = manager.activate("Jody")) {
            assertThat(ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Trudy");
                throw exception;
            }, null, null, true).handle((voidValue, throwable) -> {
                String val = manager.getActiveContextValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).whenComplete((result, throwable) -> assertContext("-Trudy")).get(), is("Trudy"));
        }
    }

    @Test
    void testHandleAsyncAndTakeSnapshot() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context ignored = manager.activate("Jody")) {
            assertThat(ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Trudy");
                throw exception;
            }).takeNewSnapshot().handleAsync((voidValue, throwable) -> {
                String val = manager.getActiveContextValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).whenComplete((result, throwable) -> assertContext("-Jody")).get(), is("Jody"));
        }

        try (Context ignored = manager.activate("Jody")) {
            assertThat(ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Trudy");
                throw exception;
            }, null, null, true).handleAsync((voidValue, throwable) -> {
                String val = manager.getActiveContextValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).whenComplete((result, throwable) -> assertContext("-Trudy")).get(), is("Trudy"));
        }
    }

    @Test
    void testHandleAsyncAndTakeSnapshot_executor() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context ignored = manager.activate("Jody")) {
            assertThat(ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Trudy");
                throw exception;
            }).takeNewSnapshot().handleAsync((voidValue, throwable) -> {
                String val = manager.getActiveContextValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }, contextUnawareThreadpool).whenComplete((result, throwable) -> assertContext("-Jody")).get(), is("Jody"));
        }

        try (Context ignored = manager.activate("Jody")) {
            assertThat(ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Trudy");
                throw exception;
            }, null, null, true).handleAsync((voidValue, throwable) -> {
                String val = manager.getActiveContextValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }, contextUnawareThreadpool).whenComplete((result, throwable) -> assertContext("-Trudy")).get(), is("Trudy"));
        }
    }

    @Test
    void testExceptionally() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Gringo")) {
            ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Jules Winnfield");
                throw new RuntimeException("Bad Motherfucker");
            }).exceptionally(ex -> {
                String val = manager.getActiveContextValue();
                assertThat(val, is("Gringo"));
                return null;
            }).get();
        }

        try (Context ignored = manager.activate("Gringo")) {
            ContextAwareCompletableFuture.runAsync(() -> {
                DummyContext.setCurrentValue("Jules Winnfield");
                throw new RuntimeException("Bad Motherfucker");
            }, null, null, true).exceptionally(ex -> {
                String val = manager.getActiveContextValue();
                assertThat(val, is("Jules Winnfield"));
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
                assertThat(val, is("Gringo"));
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
                assertThat(val, is("Jules Winnfield"));
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
            assertThat(future.get(), is("Marcellus Wallace"));
            assertThat(DummyContext.currentValue(), is("Marcellus Wallace"));
        }

        try (Context ignored = manager.activate("Marcellus Wallace")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Vincent Vega"), null, null, true)
                    .thenCombine(ContextAwareCompletableFuture
                            .runAsync(() -> DummyContext.setCurrentValue("Jules Winnfield")), (voidA, voidB) -> DummyContext.currentValue());
            assertThat(future.get(), is("Vincent Vega"));
            assertThat(DummyContext.currentValue(), is("Marcellus Wallace"));
        }
    }

    @Test
    void testThenCombineAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Marvin"))
                    .thenCombineAsync(ContextAwareCompletableFuture
                            .runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> DummyContext.currentValue());
            assertThat(future.get(), is("Brett"));
            assertThat(DummyContext.currentValue(), is("Brett"));
        }

        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Marvin"), null, null, true)
                    .thenCombineAsync(ContextAwareCompletableFuture
                            .runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> DummyContext.currentValue());
            assertThat(future.get(), is("Marvin"));
            assertThat(DummyContext.currentValue(), is("Brett"));
        }
    }

    @Test
    void testThenCombineAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Marvin"))
                    .thenCombineAsync(ContextAwareCompletableFuture
                            .runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> DummyContext.currentValue(), contextUnawareThreadpool);
            assertThat(future.get(), is("Brett"));
            assertThat(DummyContext.currentValue(), is("Brett"));
        }

        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Marvin"), null, null, true)
                    .thenCombineAsync(ContextAwareCompletableFuture
                            .runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> DummyContext.currentValue(), contextUnawareThreadpool);
            assertThat(future.get(), is("Marvin"));
            assertThat(DummyContext.currentValue(), is("Brett"));
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
                    }).whenComplete((result, exception) -> assertThat(DummyContext.currentValue(), is("-Marcellus Wallace")));

            assertThat(future.get(), is("Marcellus Wallace"));
        }

        try (Context ignored = manager.activate("Marcellus Wallace")) {
            Future<String> future = ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Vincent Vega"), null, null, true).thenCombine(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Jules Winnfield")), (voidA, voidB) -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).whenComplete((result, exception) -> assertThat(DummyContext.currentValue(), is("-Vincent Vega")));

            assertThat(future.get(), is("Vincent Vega"));
        }
    }

    @Test
    void testThenCombineAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Marvin")).takeNewSnapshot().thenCombineAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).whenComplete((result, exeption) -> assertThat(DummyContext.currentValue(), is("-Brett")));

            assertThat(future.get(), is("Brett"));
            assertThat(DummyContext.currentValue(), is("Brett"));
        }

        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Marvin"), null, null, true).thenCombineAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }).whenComplete((result, exeption) -> assertThat(DummyContext.currentValue(), is("-Marvin")));

            assertThat(future.get(), is("Marvin"));
            assertThat(DummyContext.currentValue(), is("Brett"));
        }
    }

    @Test
    void testThenCombineAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Marvin")).takeNewSnapshot().thenCombineAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }, contextUnawareThreadpool).whenComplete((result, exeption) -> assertThat(DummyContext.currentValue(), is("-Brett")));

            assertThat(future.get(), is("Brett"));
            assertThat(DummyContext.currentValue(), is("Brett"));
        }

        try (Context ignored = manager.activate("Brett")) {
            Future<String> future = ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Marvin"), null, null, true).thenCombineAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Flock of Seagulls")), (voidA, voidB) -> {
                String val = DummyContext.currentValue();
                DummyContext.setCurrentValue("-" + val);
                return val;
            }, contextUnawareThreadpool).whenComplete((result, exeption) -> assertThat(DummyContext.currentValue(), is("-Marvin")));

            assertThat(future.get(), is("Marvin"));
            assertThat(DummyContext.currentValue(), is("Brett"));
        }
    }

    @Test
    void testThenAcceptBoth() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Quentin"), null, null, true)
                    .thenAcceptBoth(completedFuture("Tarantino"), (Void voidA, String stringB) -> assertThat(manager.getActiveContextValue() + stringB, is("QuentinTarantino")))
                    .get();
            assertThat(DummyContext.currentValue(), is("Pulp Fiction"));
        }
    }

    @Test
    void testThenAcceptBothAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Quentin"), null, null, true)
                    .thenAcceptBothAsync(completedFuture("Tarantino"), (Void voidA, String stringB) -> assertThat(manager.getActiveContextValue() + stringB, is("QuentinTarantino")))
                    .get();
            assertThat(DummyContext.currentValue(), is("Pulp Fiction"));
        }
    }

    @Test
    void testThenAcceptBothAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> DummyContext.setCurrentValue("Quentin"), null, null, true)
                    .thenAcceptBothAsync(completedFuture("Tarantino"), (Void voidA, String stringB) -> assertThat(manager.getActiveContextValue() + stringB, is("QuentinTarantino")), contextUnawareThreadpool)
                    .get();
            assertThat(DummyContext.currentValue(), is("Pulp Fiction"));
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
                        assertThat(val, is("Pulp Fiction by Tarantino"));
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
                        assertThat(val, is("Quentin by Tarantino"));
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
                        assertThat(val, is("Pulp Fiction by Tarantino"));
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
                        assertThat(val, is("QuentinTarantino"));
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
                        assertThat(val, is("Pulp Fiction by Tarantino"));
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
                        assertThat(val, is("QuentinTarantino"));
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
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup")).runAfterBoth(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> assertContext("French Fries")).get();
        }

        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup"), null, null, true).runAfterBoth(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> assertContext("Ketchup")).get();
        }
    }

    @Test
    void testRunAfterBothAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup")).runAfterBothAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> assertContext("French Fries")).get();
        }

        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup"), null, null, true).runAfterBothAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> assertContext("Ketchup")).get();
        }
    }

    @Test
    void testRunAfterBothAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup")).runAfterBothAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> assertContext("French Fries"), contextUnawareThreadpool).get();
        }

        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup"), null, null, true).runAfterBothAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> assertContext("Ketchup"), contextUnawareThreadpool).get();
        }
    }

    @Test
    void testRunAfterBothAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup")).takeNewSnapshot().runAfterBoth(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> {
                String val = manager.getActiveContextValue();
                assertThat(val, is("French Fries"));
                DummyContext.setCurrentValue("-" + val);
            }).thenAccept(aVoid -> assertContext("-French Fries")).get();
        }

        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup"), null, null, true).runAfterBoth(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> {
                String val = manager.getActiveContextValue();
                assertThat(val, is("Ketchup"));
                DummyContext.setCurrentValue("-" + val);
            }).thenAccept(aVoid -> assertContext("-Ketchup")).get();
        }
    }

    @Test
    void testRunAfterBothAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup")).takeNewSnapshot().runAfterBothAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> {
                String val = manager.getActiveContextValue();
                assertThat(val, is("French Fries"));
                DummyContext.setCurrentValue("-" + val);
            }).thenAccept(aVoid -> assertContext("-French Fries")).get();
        }

        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup"), null, null, true).runAfterBothAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> {
                String val = manager.getActiveContextValue();
                assertThat(val, is("Ketchup"));
                DummyContext.setCurrentValue("-" + val);
            }).thenAccept(aVoid -> assertContext("-Ketchup")).get();
        }
    }

    @Test
    void testRunAfterBothAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup")).takeNewSnapshot().runAfterBothAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> {
                String val = manager.getActiveContextValue();
                assertThat(val, is("French Fries"));
                DummyContext.setCurrentValue("-" + val);
            }, contextUnawareThreadpool).thenAccept(aVoid -> assertContext("-French Fries")).get();
        }

        try (Context ignored = manager.activate("French Fries")) {
            ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Ketchup"), null, null, true).runAfterBothAsync(ContextAwareCompletableFuture.runAsync(() -> DummyContext.setCurrentValue("Mayonaise")), () -> {
                String val = manager.getActiveContextValue();
                assertThat(val, is("Ketchup"));
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
            assertThat(result.get(), allOf(startsWith("Winner: Function"), endsWith("Context: Parent")));
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
            assertThat(result.get(), allOf(startsWith("Winner: Function"), endsWith("Context: Parent")));
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
            assertThat(result.get(), allOf(startsWith("Winner: Function"), endsWith("Context: Parent")));
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
                assertThat(input, startsWith("Function"));
                assertContext("Parent");
            });

            // verify
            assertDoesNotThrow(() -> result.get());
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
                assertThat(input, startsWith("Function"));
                assertContext("Parent");
            });

            // verify
            assertDoesNotThrow(() -> result.get());
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
                assertThat(input, startsWith("Function"));
                assertContext("Parent");
            }, contextUnawareThreadpool);

            // verify
            assertDoesNotThrow(() -> result.get());
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
            assertDoesNotThrow(() -> result.get());
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
            assertDoesNotThrow(() -> result.get());
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
            assertDoesNotThrow(() -> result.get());
        }
    }

    @Test
    void testThenCompose() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("John")) {
            assertThat(supplyAsync(() -> {
                String current = manager.getActiveContextValue();
                DummyContext.setCurrentValue("Travolta");
                return current;
            }).thenCompose(value -> supplyAsync(() -> value + manager.getActiveContextValue())).get(), is("JohnJohn"));
        }

        try (Context ignored = manager.activate("John")) {
            assertThat(supplyAsync(() -> {
                String current = manager.getActiveContextValue();
                DummyContext.setCurrentValue("Travolta");
                return current;
            }, null, null, true).thenCompose(value -> supplyAsync(() -> value + manager.getActiveContextValue())).get(), is("JohnTravolta"));
        }
    }

    @Test
    void testThenComposeAsync() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("John")) {
            assertThat(supplyAsync(() -> {
                String current = manager.getActiveContextValue();
                DummyContext.setCurrentValue("Travolta");
                return current;
            }).thenComposeAsync(value -> supplyAsync(() -> value + manager.getActiveContextValue())).get(), is("JohnJohn"));
        }

        try (Context ignored = manager.activate("John")) {
            assertThat(supplyAsync(() -> {
                String current = manager.getActiveContextValue();
                DummyContext.setCurrentValue("Travolta");
                return current;
            }, null, null, true).thenComposeAsync(value -> supplyAsync(() -> value + manager.getActiveContextValue())).get(), is("JohnTravolta"));
        }
    }

    @Test
    void testThenComposeAsync_executor() throws ExecutionException, InterruptedException {
        try (Context ignored = manager.activate("John")) {
            assertThat(supplyAsync(() -> {
                String current = manager.getActiveContextValue();
                DummyContext.setCurrentValue("Travolta");
                return current;
            }).thenComposeAsync(value -> completedFuture(value + manager.getActiveContextValue()), contextUnawareThreadpool).get(), is("JohnJohn"));
        }

        try (Context ignored = manager.activate("John")) {
            assertThat(supplyAsync(() -> {
                String current = manager.getActiveContextValue();
                DummyContext.setCurrentValue("Travolta");
                return current;
            }, null, null, true).thenComposeAsync(value -> completedFuture(value + manager.getActiveContextValue()), contextUnawareThreadpool).get(), is("JohnTravolta"));
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

            assertThat("Future creation may not block on previous stages", future1.isDone(), is(false));
            assertThat("Future creation may not block on previous stages", future2.isDone(), is(false));
            assertThat("Future creation may not block on previous stages", future3.isDone(), is(false));

            latch1.countDown();
            future1.get(500, TimeUnit.MILLISECONDS);
            assertThat("Future creation may not block on previous stages", future1.isDone(), is(true));
            assertThat("Future creation may not block on previous stages", future2.isDone(), is(false));
            assertThat("Future creation may not block on previous stages", future3.isDone(), is(false));

            latch2.countDown();
            future2.get(500, TimeUnit.MILLISECONDS);
            assertThat("Future creation may not block on previous stages", future2.isDone(), is(true));
            assertThat(future3.get(500, TimeUnit.MILLISECONDS), is("Vincent Vega, Jules Winnfield, Marcellus Wallace"));
            assertThat(DummyContext.currentValue(), is("Vincent Vega"));
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
        assertThat(result.isDone(), is(false));
        cf1.complete("Value 1");
        assertThat(result.isDone(), is(false));
        cf2.complete("Value 2");
        assertThat(result.get(), is("Vincent Vega"));
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
        assertThat(result.isDone(), is(false));
        cf1.complete("Value 1");
        assertThat(result.isDone(), is(false));
        cf2.complete("Value 2");
        assertThat(result.get(), is("Vincent Vega"));
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
        assertThat(result.isDone(), is(false));
        cf2.complete("Value 2");
        assertThat(result.get(), is("Vincent Vega"));
        assertThat(future.get(), is("Value 2"));
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
        assertThat(result.isDone(), is(false));
        cf1.complete("Value 1");
        assertThat(result.get(), is("Vincent Vega"));
        assertThat(future.get(), is("Value 1"));
    }

    static void waitFor(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted waiting for latch.", ie);
        }
    }
}
