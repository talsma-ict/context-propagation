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
package nl.talsmasoftware.context.futures;

import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.DummyContextManager;
import nl.talsmasoftware.context.api.Context;
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
import static nl.talsmasoftware.context.futures.ContextAwareCompletableFuture.supplyAsync;
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
public class ContextAwareCompletableFutureTest {
    private static final Random RND = new Random();
    private static final DummyContextManager manager = new DummyContextManager();
    private static final ExecutorService contextUnawareThreadpool = Executors.newCachedThreadPool();

    @BeforeEach
    @AfterEach
    public void clearDummyContext() {
        DummyContextManager.clear();
    }

    private static void assertContext(String expectedValue) {
        assertThat("Active context value", manager.getActiveContext().getValue(), is(expectedValue));
    }

    private static Supplier<String> stringSupplier(String name, String requiredContext) {
        return () -> {
            assertContext(requiredContext);
            manager.initializeNewContext(name); // intentionally not closed here
            assertContext(name);
            assertDoesNotThrow(() -> Thread.sleep(50 + RND.nextInt(200)));
            return String.format(name + " (Thread: %s)", Thread.currentThread().getName());
        };
    }

    @Test
    public void testDefaultConstructor() throws ExecutionException, InterruptedException {
        Context<String> ctx = manager.initializeNewContext("foo");
        CompletableFuture<String> future1 = new ContextAwareCompletableFuture<>(); // should have a new snapshot with foo
        ctx.close();

        CompletableFuture<String> future2 = future1.thenApply(value -> manager.getActiveContext().getValue() + value);
        assertThat(manager.getActiveContext(), is(nullValue()));
        assertThat(future1.isDone(), is(false));
        assertThat(future2.isDone(), is(false));

        assertThat(future1.complete("bar"), is(true));
        assertThat(future1.isDone(), is(true));
        assertThat(future2.isDone(), is(true));
        assertThat(future2.get(), is("foobar"));
    }

    @Test
    public void testSupplyAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Vincent Vega")) {
            Future<Optional<String>> future = supplyAsync(DummyContextManager::currentValue);
            assertThat(future.get().get(), is("Vincent Vega"));
        }
    }

    @Test
    public void testSupplyAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Marcellus Wallace")) {
            Future<Optional<String>> future = supplyAsync(DummyContextManager::currentValue, contextUnawareThreadpool);
            assertThat(future.get().get(), is("Marcellus Wallace"));
        }
    }

    @Test
    public void testSupplyAsync_executor_snapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Vincent Vega")) {
            ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
            manager.initializeNewContext("Jules Winnfield");
            assertContext("Jules Winnfield");

            Future<Optional<String>> future = supplyAsync(DummyContextManager::currentValue, contextUnawareThreadpool, snapshot);
            assertThat(future.get().get(), is("Vincent Vega"));
        }
    }

    @Test
    public void testSupplyAsync_executor_snapshot_takeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Vincent Vega")) {
            ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
            manager.initializeNewContext("Jules Winnfield");
            assertContext("Jules Winnfield");

            ContextAwareCompletableFuture<Optional<String>> future = supplyAsync(() -> {
                try {
                    return DummyContextManager.currentValue();
                } finally {
                    DummyContextManager.setCurrentValue("Marcellus Wallace");
                }
            }, contextUnawareThreadpool, snapshot, true);

            assertThat(future.get(), is(Optional.of("Vincent Vega")));
            assertThat(future.thenApply(x -> DummyContextManager.currentValue()).get(),
                    is(Optional.of("Marcellus Wallace")));
        }
    }

    @Test
    public void testRunAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Mia Wallace")) {
            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Mia Wallace"))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testRunAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Jimmie")) {
            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(
                    () -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Jimmie"))),
                    contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testRunAsync_executor_snapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Pumpkin")) {
            ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
            manager.initializeNewContext("Honey Bunny");
            assertContext("Honey Bunny");

            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(
                    () -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Pumpkin"))),
                    contextUnawareThreadpool,
                    snapshot);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testRunAsync_executor_snapshot_takeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Pumpkin")) {
            ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
            manager.initializeNewContext("Honey Bunny");
            assertContext("Honey Bunny");

            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(
                    () -> {
                        try {
                            assertThat(DummyContextManager.currentValue(), is(Optional.of("Pumpkin")));
                        } finally {
                            DummyContextManager.setCurrentValue("Bad Motherfucker");
                        }
                    },
                    contextUnawareThreadpool,
                    snapshot,
                    true);
            future.get(); // trigger asynchronous assertion
            future.thenRun(() -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Bad Motherfucker")))).get();
        }
    }

    @Test
    public void testThenApply() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Jimmie")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Bonnie"))
                    .thenApply(voidvalue -> DummyContextManager.currentValue());
            assertThat(future.get(), is(Optional.of("Jimmie")));
        }

        try (Context<String> ctx = manager.initializeNewContext("Jimmie")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Bonnie"), null, null, true)
                    .thenApply(voidvalue -> DummyContextManager.currentValue());
            assertThat(future.get(), is(Optional.of("Bonnie")));
        }
    }

    @Test
    public void testThenApplyAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Esmerelda Villalobos"))
                    .thenApplyAsync(voidvalue -> DummyContextManager.currentValue());
            assertThat(future.get().get(), is("Butch"));
        }

        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Esmerelda Villalobos"), null, null, true)
                    .thenApplyAsync(voidvalue -> DummyContextManager.currentValue());
            assertThat(future.get().get(), is("Esmerelda Villalobos"));
        }
    }

    @Test
    public void testThenApplyAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Maynard")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Zed"))
                    .thenApplyAsync(voidvalue -> DummyContextManager.currentValue(), contextUnawareThreadpool);
            assertThat(future.get().get(), is("Maynard"));
        }

        try (Context<String> ctx = manager.initializeNewContext("Maynard")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Zed"), null, null, true)
                    .thenApplyAsync(voidvalue -> DummyContextManager.currentValue(), contextUnawareThreadpool);
            assertThat(future.get().get(), is("Zed"));
        }
    }

    @Test
    public void testThenApplyAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Jimmie")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Bonnie"))
                    .takeNewSnapshot()
                    .thenApply(voidvalue -> {
                        String val = DummyContextManager.currentValue().get();
                        manager.initializeNewContext("-" + val);
                        return val;
                    })
                    .thenAccept(s -> {
                        assertThat(s, is("Jimmie"));
                        assertThat(DummyContextManager.currentValue(), is(Optional.of("-Jimmie")));
                    })
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("Jimmie")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Bonnie"), null, null, true)
                    .thenApply(voidvalue -> {
                        String val = DummyContextManager.currentValue().get();
                        manager.initializeNewContext("-" + val);
                        return val;
                    })
                    .thenAccept(s -> {
                        assertThat(s, is("Bonnie"));
                        assertThat(DummyContextManager.currentValue(), is(Optional.of("-Bonnie")));
                    })
                    .get();
        }
    }

    @Test
    public void testThenApplyAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Esmerelda Villalobos"))
                    .takeNewSnapshot()
                    .thenApplyAsync(voidvalue -> {
                        String val = DummyContextManager.currentValue().get();
                        manager.initializeNewContext("-" + val);
                        return val;
                    })
                    .thenAccept(s -> {
                        assertThat(s, is("Butch"));
                        assertThat(DummyContextManager.currentValue(), is(Optional.of("-Butch")));
                    })
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Esmerelda Villalobos"), null, null, true)
                    .thenApplyAsync(voidvalue -> {
                        String val = DummyContextManager.currentValue().get();
                        manager.initializeNewContext("-" + val);
                        return val;
                    })
                    .thenAccept(s -> {
                        assertThat(s, is("Esmerelda Villalobos"));
                        assertThat(DummyContextManager.currentValue(), is(Optional.of("-Esmerelda Villalobos")));
                    })
                    .get();
        }
    }

    @Test
    public void testThenApplyAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Maynard")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Zed"))
                    .takeNewSnapshot()
                    .thenApplyAsync(voidvalue -> {
                        String val = DummyContextManager.currentValue().get();
                        manager.initializeNewContext("-" + val);
                        return val;
                    }, contextUnawareThreadpool)
                    .thenAccept(s -> {
                        assertThat(s, is("Maynard"));
                        assertThat(DummyContextManager.currentValue(), is(Optional.of("-Maynard")));
                    })
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("Maynard")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Zed"), null, null, true)
                    .thenApplyAsync(voidvalue -> {
                        String val = DummyContextManager.currentValue().get();
                        manager.initializeNewContext("-" + val);
                        return val;
                    }, contextUnawareThreadpool)
                    .thenAccept(s -> {
                        assertThat(s, is("Zed"));
                        assertThat(DummyContextManager.currentValue(), is(Optional.of("-Zed")));
                    })
                    .get();
        }
    }

    @Test
    public void testThenAccept() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("The Gimp")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Butch"))
                    .thenAccept(voidvalue -> assertThat(DummyContextManager.currentValue(), is(Optional.of("The Gimp"))));
            future.get(); // trigger asynchronous assertion
        }

        try (Context<String> ctx = manager.initializeNewContext("The Gimp")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Butch"), null, null, true)
                    .thenAccept(voidvalue -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Butch"))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenAcceptAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Fabienne"))
                    .thenAcceptAsync(voidvalue -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Butch"))));
            future.get(); // trigger asynchronous assertion
        }

        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Fabienne"), null, null, true)
                    .thenAcceptAsync(voidvalue -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Fabienne"))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenAcceptAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Marvin")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Winston Wolfe"))
                    .thenAcceptAsync(
                            voidvalue -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Marvin"))),
                            contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }

        try (Context<String> ctx = manager.initializeNewContext("Marvin")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Winston Wolfe"), null, null, true)
                    .thenAcceptAsync(
                            voidvalue -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Winston Wolfe"))),
                            contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenAcceptAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("The Gimp")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Butch"))
                    .takeNewSnapshot()
                    .thenAccept(voidvalue -> {
                        String val = DummyContextManager.currentValue().get();
                        assertThat(val, is("The Gimp"));
                        manager.initializeNewContext("-" + val);
                    })
                    .thenAccept(aVoid -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-The Gimp"))))
                    .get(); // trigger asynchronous assertion
        }

        try (Context<String> ctx = manager.initializeNewContext("The Gimp")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Butch"), null, null, true)
                    .thenAccept(voidvalue -> {
                        String val = DummyContextManager.currentValue().get();
                        assertThat(val, is("Butch"));
                        manager.initializeNewContext("-" + val);
                    })
                    .thenAccept(aVoid -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Butch"))))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenAcceptAsynAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Fabienne"))
                    .takeNewSnapshot()
                    .thenAcceptAsync(voidvalue -> {
                        String val = DummyContextManager.currentValue().get();
                        assertThat(val, is("Butch"));
                        manager.initializeNewContext("-" + val);
                    })
                    .thenAccept(aVoid -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Butch"))))
                    .get(); // trigger asynchronous assertion
        }

        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Fabienne"), null, null, true)
                    .thenAcceptAsync(voidvalue -> {
                        String val = DummyContextManager.currentValue().get();
                        assertThat(val, is("Fabienne"));
                        manager.initializeNewContext("-" + val);
                    })
                    .thenAccept(aVoid -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Fabienne"))))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenAcceptAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Marvin")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Winston Wolfe"))
                    .takeNewSnapshot()
                    .thenAcceptAsync(
                            voidvalue -> {
                                String val = DummyContextManager.currentValue().get();
                                assertThat(val, is("Marvin"));
                                manager.initializeNewContext("-" + val);
                            },
                            contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Marvin"))))
                    .get(); // trigger asynchronous assertion
        }

        try (Context<String> ctx = manager.initializeNewContext("Marvin")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Winston Wolfe"), null, null, true)
                    .thenAcceptAsync(
                            voidvalue -> {
                                String val = DummyContextManager.currentValue().get();
                                assertThat(val, is("Winston Wolfe"));
                                manager.initializeNewContext("-" + val);
                            },
                            contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Winston Wolfe"))))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenRun() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Lance")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Jody"))
                    .thenRun(() -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Lance"))));
            future.get(); // trigger asynchronous assertion
        }

        try (Context<String> ctx = manager.initializeNewContext("Lance")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Jody"), null, null, true)
                    .thenRun(() -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Jody"))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenRunAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Ringo")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Yolanda"))
                    .thenRunAsync(() -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Ringo"))));
            future.get(); // trigger asynchronous assertion
        }

        try (Context<String> ctx = manager.initializeNewContext("Ringo")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Yolanda"), null, null, true)
                    .thenRunAsync(() -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Yolanda"))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenRunAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Capt. Koons")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Butch"))
                    .thenRunAsync(
                            () -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Capt. Koons"))),
                            contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }

        try (Context<String> ctx = manager.initializeNewContext("Capt. Koons")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Butch"), null, null, true)
                    .thenRunAsync(
                            () -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Butch"))),
                            contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenRunAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Lance")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Jody"))
                    .takeNewSnapshot()
                    .thenRun(() -> {
                        String val = DummyContextManager.currentValue().get();
                        assertThat(val, is("Lance"));
                        manager.initializeNewContext("-" + val);
                    })
                    .thenAccept(aVoid -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Lance"))))
                    .get(); // trigger asynchronous assertion
        }

        try (Context<String> ctx = manager.initializeNewContext("Lance")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Jody"), null, null, true)
                    .thenRun(() -> {
                        String val = DummyContextManager.currentValue().get();
                        assertThat(val, is("Jody"));
                        manager.initializeNewContext("-" + val);
                    })
                    .thenAccept(aVoid -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Jody"))))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenRunAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Ringo")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Yolanda"))
                    .takeNewSnapshot()
                    .thenRunAsync(() -> {
                        String val = DummyContextManager.currentValue().get();
                        assertThat(val, is("Ringo"));
                        manager.initializeNewContext("-" + val);
                    })
                    .thenAccept(aVoid -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Ringo"))))
                    .get(); // trigger asynchronous assertion
        }

        try (Context<String> ctx = manager.initializeNewContext("Ringo")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Yolanda"), null, null, true)
                    .thenRunAsync(() -> {
                        String val = DummyContextManager.currentValue().get();
                        assertThat(val, is("Yolanda"));
                        manager.initializeNewContext("-" + val);
                    })
                    .thenAccept(aVoid -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Yolanda"))))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenRunAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Capt. Koons")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Butch"))
                    .takeNewSnapshot()
                    .thenRunAsync(
                            () -> {
                                String val = DummyContextManager.currentValue().get();
                                assertThat(val, is("Capt. Koons"));
                                manager.initializeNewContext("-" + val);
                            },
                            contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Capt. Koons"))))
                    .get(); // trigger asynchronous assertion
        }

        try (Context<String> ctx = manager.initializeNewContext("Capt. Koons")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Butch"), null, null, true)
                    .thenRunAsync(
                            () -> {
                                String val = DummyContextManager.currentValue().get();
                                assertThat(val, is("Butch"));
                                manager.initializeNewContext("-" + val);
                            },
                            contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Butch"))))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testWhenComplete() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Floyd"))
                    .whenComplete((voidValue, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Butch"))))
                    .get(); // trigger asynchronous assertion
        }

        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Floyd"), null, null, true)
                    .whenComplete((voidValue, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Floyd"))))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testWhenCompleteAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Zed")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Pipe hittin' niggers"))
                    .whenCompleteAsync((voidValue, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Zed"))))
                    .get(); // trigger asynchronous assertion
        }

        try (Context<String> ctx = manager.initializeNewContext("Zed")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Pipe hittin' niggers"), null, null, true)
                    .whenCompleteAsync((voidValue, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Pipe hittin' niggers"))))
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testWhenCompleteAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Floyd"))
                    .whenCompleteAsync(
                            (voidValue, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Butch"))),
                            contextUnawareThreadpool)
                    .get(); // trigger asynchronous assertion
        }

        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Floyd"), null, null, true)
                    .whenCompleteAsync(
                            (voidValue, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Floyd"))),
                            contextUnawareThreadpool)
                    .get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testHandle() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            assertThat(ContextAwareCompletableFuture
                            .runAsync(() -> {
                                manager.initializeNewContext("Trudy");
                                throw exception;
                            })
                            .handleAsync((voidValue, throwable) -> manager.getActiveContext().getValue())
                            .get(),
                    is("Jody"));
        }

        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            assertThat(ContextAwareCompletableFuture
                            .runAsync(() -> {
                                manager.initializeNewContext("Trudy");
                                throw exception;
                            }, null, null, true)
                            .handleAsync((voidValue, throwable) -> manager.getActiveContext().getValue())
                            .get(),
                    is("Trudy"));
        }
    }

    @Test
    public void testHandleAsync() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            assertThat(ContextAwareCompletableFuture
                            .runAsync(() -> {
                                manager.initializeNewContext("Trudy");
                                throw exception;
                            })
                            .handleAsync((voidValue, throwable) -> manager.getActiveContext().getValue())
                            .get(),
                    is("Jody"));
        }

        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            assertThat(ContextAwareCompletableFuture
                            .runAsync(() -> {
                                manager.initializeNewContext("Trudy");
                                throw exception;
                            }, null, null, true)
                            .handleAsync((voidValue, throwable) -> manager.getActiveContext().getValue())
                            .get(),
                    is("Trudy"));
        }
    }

    @Test
    public void testHandleAsync_executor() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            assertThat(ContextAwareCompletableFuture
                            .runAsync(() -> {
                                manager.initializeNewContext("Trudy");
                                throw exception;
                            })
                            .handleAsync(
                                    (voidValue, throwable) -> manager.getActiveContext().getValue(),
                                    contextUnawareThreadpool)
                            .get(),
                    is("Jody"));
        }

        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            assertThat(ContextAwareCompletableFuture
                            .runAsync(() -> {
                                manager.initializeNewContext("Trudy");
                                throw exception;
                            }, null, null, true)
                            .handleAsync(
                                    (voidValue, throwable) -> manager.getActiveContext().getValue(),
                                    contextUnawareThreadpool)
                            .get(),
                    is("Trudy"));
        }
    }

    @Test
    public void testHandleAndTakeSnapshot() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            assertThat(ContextAwareCompletableFuture
                            .runAsync(() -> {
                                manager.initializeNewContext("Trudy");
                                throw exception;
                            })
                            .takeNewSnapshot()
                            .handle((voidValue, throwable) -> {
                                String val = manager.getActiveContext().getValue();
                                manager.initializeNewContext("-" + val);
                                return val;
                            })
                            .whenComplete((result, throwable) -> assertContext("-Jody"))
                            .get(),
                    is("Jody"));
        }

        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            assertThat(ContextAwareCompletableFuture
                            .runAsync(() -> {
                                manager.initializeNewContext("Trudy");
                                throw exception;
                            }, null, null, true)
                            .handle((voidValue, throwable) -> {
                                String val = manager.getActiveContext().getValue();
                                manager.initializeNewContext("-" + val);
                                return val;
                            })
                            .whenComplete((result, throwable) -> assertContext("-Trudy"))
                            .get(),
                    is("Trudy"));
        }
    }

    @Test
    public void testHandleAsyncAndTakeSnapshot() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            assertThat(ContextAwareCompletableFuture
                            .runAsync(() -> {
                                manager.initializeNewContext("Trudy");
                                throw exception;
                            })
                            .takeNewSnapshot()
                            .handleAsync((voidValue, throwable) -> {
                                String val = manager.getActiveContext().getValue();
                                manager.initializeNewContext("-" + val);
                                return val;
                            })
                            .whenComplete((result, throwable) -> assertContext("-Jody"))
                            .get(),
                    is("Jody"));
        }

        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            assertThat(ContextAwareCompletableFuture
                            .runAsync(() -> {
                                manager.initializeNewContext("Trudy");
                                throw exception;
                            }, null, null, true)
                            .handleAsync((voidValue, throwable) -> {
                                String val = manager.getActiveContext().getValue();
                                manager.initializeNewContext("-" + val);
                                return val;
                            })
                            .whenComplete((result, throwable) -> assertContext("-Trudy"))
                            .get(),
                    is("Trudy"));
        }
    }

    @Test
    public void testHandleAsyncAndTakeSnapshot_executor() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            assertThat(ContextAwareCompletableFuture
                            .runAsync(() -> {
                                manager.initializeNewContext("Trudy");
                                throw exception;
                            })
                            .takeNewSnapshot()
                            .handleAsync((voidValue, throwable) -> {
                                String val = manager.getActiveContext().getValue();
                                manager.initializeNewContext("-" + val);
                                return val;
                            }, contextUnawareThreadpool)
                            .whenComplete((result, throwable) -> assertContext("-Jody"))
                            .get(),
                    is("Jody"));
        }

        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            assertThat(ContextAwareCompletableFuture
                            .runAsync(() -> {
                                manager.initializeNewContext("Trudy");
                                throw exception;
                            }, null, null, true)
                            .handleAsync((voidValue, throwable) -> {
                                String val = manager.getActiveContext().getValue();
                                manager.initializeNewContext("-" + val);
                                return val;
                            }, contextUnawareThreadpool)
                            .whenComplete((result, throwable) -> assertContext("-Trudy"))
                            .get(),
                    is("Trudy"));
        }
    }

    @Test
    public void testExceptionally() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Gringo")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> {
                        manager.initializeNewContext("Jules Winnfield");
                        throw new RuntimeException("Bad Motherfucker");
                    })
                    .exceptionally(ex -> {
                        String val = manager.getActiveContext().getValue();
                        assertThat(val, is("Gringo"));
                        return null;
                    })
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("Gringo")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> {
                        manager.initializeNewContext("Jules Winnfield");
                        throw new RuntimeException("Bad Motherfucker");
                    }, null, null, true)
                    .exceptionally(ex -> {
                        String val = manager.getActiveContext().getValue();
                        assertThat(val, is("Jules Winnfield"));
                        return null;
                    })
                    .get();
        }
    }

    @Test
    public void testExceptionallyAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Gringo")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> {
                        manager.initializeNewContext("Jules Winnfield");
                        throw new RuntimeException("Bad Motherfucker");
                    })
                    .takeNewSnapshot()
                    .exceptionally(ex -> {
                        String val = manager.getActiveContext().getValue();
                        assertThat(val, is("Gringo"));
                        manager.initializeNewContext("-" + val);
                        return null;
                    })
                    .thenAccept(aVoid -> assertContext("-Gringo"))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("Gringo")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> {
                        manager.initializeNewContext("Jules Winnfield");
                        throw new RuntimeException("Bad Motherfucker");
                    }, null, null, true)
                    .exceptionally(ex -> {
                        String val = manager.getActiveContext().getValue();
                        assertThat(val, is("Jules Winnfield"));
                        manager.initializeNewContext("-" + val);
                        return null;
                    })
                    .thenAccept(aVoid -> assertContext("-Jules Winnfield"))
                    .get();
        }
    }

    @Test
    public void testThenCombine() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Marcellus Wallace")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Vincent Vega"))
                    .thenCombine(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Jules Winnfield")),
                            (voidA, voidB) -> DummyContextManager.currentValue());
            assertThat(future.get(), is(Optional.of("Marcellus Wallace")));
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Marcellus Wallace")));
        }

        try (Context<String> ctx = manager.initializeNewContext("Marcellus Wallace")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Vincent Vega"), null, null, true)
                    .thenCombine(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Jules Winnfield")),
                            (voidA, voidB) -> DummyContextManager.currentValue());
            assertThat(future.get(), is(Optional.of("Vincent Vega")));
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Marcellus Wallace")));
        }
    }

    @Test
    public void testThenCombineAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Brett")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Marvin"))
                    .thenCombineAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Flock of Seagulls")),
                            (voidA, voidB) -> DummyContextManager.currentValue());
            assertThat(future.get(), is(Optional.of("Brett")));
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Brett")));
        }

        try (Context<String> ctx = manager.initializeNewContext("Brett")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Marvin"), null, null, true)
                    .thenCombineAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Flock of Seagulls")),
                            (voidA, voidB) -> DummyContextManager.currentValue());
            assertThat(future.get(), is(Optional.of("Marvin")));
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Brett")));
        }
    }

    @Test
    public void testThenCombineAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Brett")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Marvin"))
                    .thenCombineAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Flock of Seagulls")),
                            (voidA, voidB) -> DummyContextManager.currentValue(),
                            contextUnawareThreadpool);
            assertThat(future.get(), is(Optional.of("Brett")));
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Brett")));
        }

        try (Context<String> ctx = manager.initializeNewContext("Brett")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Marvin"), null, null, true)
                    .thenCombineAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Flock of Seagulls")),
                            (voidA, voidB) -> DummyContextManager.currentValue(),
                            contextUnawareThreadpool);
            assertThat(future.get(), is(Optional.of("Marvin")));
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Brett")));
        }
    }

    @Test
    public void testThenCombineAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Marcellus Wallace")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Vincent Vega"))
                    .takeNewSnapshot()
                    .thenCombine(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Jules Winnfield")),
                            (voidA, voidB) -> {
                                String val = DummyContextManager.currentValue().get();
                                manager.initializeNewContext("-" + val);
                                return val;
                            })
                    .whenComplete((result, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Marcellus Wallace"))));

            assertThat(future.get(), is("Marcellus Wallace"));
        }

        try (Context<String> ctx = manager.initializeNewContext("Marcellus Wallace")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Vincent Vega"), null, null, true)
                    .thenCombine(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Jules Winnfield")),
                            (voidA, voidB) -> {
                                String val = DummyContextManager.currentValue().get();
                                manager.initializeNewContext("-" + val);
                                return val;
                            })
                    .whenComplete((result, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Vincent Vega"))));

            assertThat(future.get(), is("Vincent Vega"));
        }
    }

    @Test
    public void testThenCombineAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Brett")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Marvin"))
                    .takeNewSnapshot()
                    .thenCombineAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Flock of Seagulls")),
                            (voidA, voidB) -> {
                                String val = DummyContextManager.currentValue().get();
                                manager.initializeNewContext("-" + val);
                                return val;
                            })
                    .whenComplete((result, exeption) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Brett"))));

            assertThat(future.get(), is("Brett"));
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Brett")));
        }

        try (Context<String> ctx = manager.initializeNewContext("Brett")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Marvin"), null, null, true)
                    .thenCombineAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Flock of Seagulls")),
                            (voidA, voidB) -> {
                                String val = DummyContextManager.currentValue().get();
                                manager.initializeNewContext("-" + val);
                                return val;
                            })
                    .whenComplete((result, exeption) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Marvin"))));

            assertThat(future.get(), is("Marvin"));
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Brett")));
        }
    }

    @Test
    public void testThenCombineAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Brett")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Marvin"))
                    .takeNewSnapshot()
                    .thenCombineAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Flock of Seagulls")),
                            (voidA, voidB) -> {
                                String val = DummyContextManager.currentValue().get();
                                manager.initializeNewContext("-" + val);
                                return val;
                            },
                            contextUnawareThreadpool)
                    .whenComplete((result, exeption) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Brett"))));

            assertThat(future.get(), is("Brett"));
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Brett")));
        }

        try (Context<String> ctx = manager.initializeNewContext("Brett")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Marvin"), null, null, true)
                    .thenCombineAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Flock of Seagulls")),
                            (voidA, voidB) -> {
                                String val = DummyContextManager.currentValue().get();
                                manager.initializeNewContext("-" + val);
                                return val;
                            },
                            contextUnawareThreadpool)
                    .whenComplete((result, exeption) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("-Marvin"))));

            assertThat(future.get(), is("Marvin"));
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Brett")));
        }
    }

    @Test
    public void testThenAcceptBoth() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Quentin").getValue(), null, null, true)
                    .thenAcceptBoth(completedFuture("Tarantino"),
                            (Void voidA, String stringB) ->
                                    assertThat(manager.getActiveContext().getValue() + stringB,
                                            is("QuentinTarantino")))
                    .get();
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Pulp Fiction")));
        }
    }

    @Test
    public void testThenAcceptBothAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Quentin").getValue(), null, null, true)
                    .thenAcceptBothAsync(completedFuture("Tarantino"),
                            (Void voidA, String stringB) ->
                                    assertThat(manager.getActiveContext().getValue() + stringB,
                                            is("QuentinTarantino")))
                    .get();
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Pulp Fiction")));
        }
    }

    @Test
    public void testThenAcceptBothAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Quentin").getValue(), null, null, true)
                    .thenAcceptBothAsync(completedFuture("Tarantino"),
                            (Void voidA, String stringB) ->
                                    assertThat(manager.getActiveContext().getValue() + stringB,
                                            is("QuentinTarantino")),
                            contextUnawareThreadpool)
                    .get();
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Pulp Fiction")));
        }
    }

    @Test
    public void testThenAcceptBothAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Quentin").getValue())
                    .takeNewSnapshot()
                    .thenAcceptBoth(completedFuture(" by Tarantino"),
                            (Void voidA, String stringB) -> {
                                String val = manager.getActiveContext().getValue() + stringB;
                                assertThat(val, is("Pulp Fiction by Tarantino"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertContext("-Pulp Fiction by Tarantino"))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Quentin").getValue(), null, null, true)
                    .thenAcceptBoth(completedFuture(" by Tarantino"),
                            (Void voidA, String stringB) -> {
                                String val = manager.getActiveContext().getValue() + stringB;
                                assertThat(val, is("Quentin by Tarantino"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertContext("-Quentin by Tarantino"))
                    .get();
        }
    }

    @Test
    public void testThenAcceptBothAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Quentin").getValue())
                    .takeNewSnapshot()
                    .thenAcceptBoth(completedFuture(" by Tarantino"),
                            (Void voidA, String stringB) -> {
                                String val = manager.getActiveContext().getValue() + stringB;
                                assertThat(val, is("Pulp Fiction by Tarantino"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertContext("-Pulp Fiction by Tarantino"))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Quentin").getValue(), null, null, true)
                    .thenAcceptBoth(completedFuture("Tarantino"),
                            (Void voidA, String stringB) -> {
                                String val = manager.getActiveContext().getValue() + stringB;
                                assertThat(val, is("QuentinTarantino"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertContext("-QuentinTarantino"))
                    .get();
        }
    }

    @Test
    public void testThenAcceptBothAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Quentin").getValue())
                    .takeNewSnapshot()
                    .thenAcceptBothAsync(completedFuture(" by Tarantino"),
                            (Void voidA, String stringB) -> {
                                String val = manager.getActiveContext().getValue() + stringB;
                                assertThat(val, is("Pulp Fiction by Tarantino"));
                                manager.initializeNewContext("-" + val);
                            },
                            contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertContext("-Pulp Fiction by Tarantino"))
                    .get();
            assertContext("Pulp Fiction");
        }

        try (Context<String> ctx = manager.initializeNewContext("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Quentin").getValue(), null, null, true)
                    .thenAcceptBothAsync(completedFuture("Tarantino"),
                            (Void voidA, String stringB) -> {
                                String val = manager.getActiveContext().getValue() + stringB;
                                assertThat(val, is("QuentinTarantino"));
                                manager.initializeNewContext("-" + val);
                            },
                            contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertContext("-QuentinTarantino"))
                    .get();
            assertContext("Pulp Fiction");
        }
    }

    @Test
    public void testRunAfterBoth() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"))
                    .runAfterBoth(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> assertContext("French Fries"))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"), null, null, true)
                    .runAfterBoth(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> assertContext("Ketchup"))
                    .get();
        }
    }

    @Test
    public void testRunAfterBothAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"))
                    .runAfterBothAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> assertContext("French Fries"))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"), null, null, true)
                    .runAfterBothAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> assertContext("Ketchup"))
                    .get();
        }
    }

    @Test
    public void testRunAfterBothAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"))
                    .runAfterBothAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> assertContext("French Fries"),
                            contextUnawareThreadpool)
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"), null, null, true)
                    .runAfterBothAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> assertContext("Ketchup"),
                            contextUnawareThreadpool)
                    .get();
        }
    }

    @Test
    public void testRunAfterBothAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"))
                    .takeNewSnapshot()
                    .runAfterBoth(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("French Fries"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertContext("-French Fries"))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"), null, null, true)
                    .runAfterBoth(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("Ketchup"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertContext("-Ketchup"))
                    .get();
        }
    }

    @Test
    public void testRunAfterBothAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"))
                    .takeNewSnapshot()
                    .runAfterBothAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("French Fries"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertContext("-French Fries"))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"), null, null, true)
                    .runAfterBothAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("Ketchup"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertContext("-Ketchup"))
                    .get();
        }
    }

    @Test
    public void testRunAfterBothAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"))
                    .takeNewSnapshot()
                    .runAfterBothAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("French Fries"));
                                manager.initializeNewContext("-" + val);
                            },
                            contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertContext("-French Fries"))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"), null, null, true)
                    .runAfterBothAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("Ketchup"));
                                manager.initializeNewContext("-" + val);
                            },
                            contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertContext("-Ketchup"))
                    .get();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testApplyToEither(boolean takeNewSnapshot) throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<String> result = future1
                    .takeNewSnapshot(takeNewSnapshot)
                    .applyToEither(future2,
                            input -> String.format("Winner: %s, Thread: %s, Context: %s",
                                    input, Thread.currentThread().getName(), manager.getActiveContext().getValue()));

            // verify
            assertThat(result.get(), allOf(startsWith("Winner: Function"), endsWith("Context: Parent")));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testApplyToEitherAsync(boolean takeNewSnapshot) throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<String> result = future1
                    .takeNewSnapshot(takeNewSnapshot)
                    .applyToEitherAsync(
                            future2,
                            input -> String.format("Winner: %s, Thread: %s, Context: %s",
                                    input, Thread.currentThread().getName(), manager.getActiveContext().getValue()));

            // verify
            assertThat(result.get(), allOf(startsWith("Winner: Function"), endsWith("Context: Parent")));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testApplyToEitherAsync_executor(boolean takeNewSnapshot) throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<String> result = future1
                    .takeNewSnapshot(takeNewSnapshot)
                    .applyToEitherAsync(
                            future2,
                            input -> String.format("Winner: %s, Thread: %s, Context: %s",
                                    input, Thread.currentThread().getName(), manager.getActiveContext().getValue()),
                            contextUnawareThreadpool);

            // verify
            assertThat(result.get(), allOf(startsWith("Winner: Function"), endsWith("Context: Parent")));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testAcceptEither(boolean takeNewSnapshot) {
        try (Context<String> ctx = manager.initializeNewContext("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<Void> result = future1.takeNewSnapshot(takeNewSnapshot)
                    .acceptEither(
                            future2,
                            input -> {
                                assertThat(input, startsWith("Function"));
                                assertContext("Parent");
                            });

            // verify
            assertDoesNotThrow(() -> result.get());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testAcceptEitherAsync(boolean takeNewSnapshot) {
        try (Context<String> ctx = manager.initializeNewContext("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<Void> result = future1.takeNewSnapshot(takeNewSnapshot)
                    .acceptEitherAsync(
                            future2,
                            input -> {
                                assertThat(input, startsWith("Function"));
                                assertContext("Parent");
                            });

            // verify
            assertDoesNotThrow(() -> result.get());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testAcceptEitherAsync_executor(boolean takeNewSnapshot) {
        try (Context<String> ctx = manager.initializeNewContext("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<Void> result = future1.takeNewSnapshot(takeNewSnapshot)
                    .acceptEitherAsync(
                            future2,
                            input -> {
                                assertThat(input, startsWith("Function"));
                                assertContext("Parent");
                            },
                            contextUnawareThreadpool);

            // verify
            assertDoesNotThrow(() -> result.get());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testRunAfterEither(boolean takeNewSnapshot) {
        try (Context<String> ctx = manager.initializeNewContext("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<Void> result = future1.takeNewSnapshot(takeNewSnapshot)
                    .runAfterEither(future2, () -> assertContext("Parent"));

            // verify
            assertDoesNotThrow(() -> result.get());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testRunAfterEitherAsync(boolean takeNewSnapshot) {
        try (Context<String> ctx = manager.initializeNewContext("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<Void> result = future1.takeNewSnapshot(takeNewSnapshot)
                    .runAfterEitherAsync(future2, () -> assertContext("Parent"));

            // verify
            assertDoesNotThrow(() -> result.get());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testRunAfterEitherAsync_executor(boolean takeNewSnapshot) {
        try (Context<String> ctx = manager.initializeNewContext("Parent")) {
            // prepare
            ContextAwareCompletableFuture<String> future1 = supplyAsync(stringSupplier("Function 1", "Parent"));
            ContextAwareCompletableFuture<String> future2 = supplyAsync(stringSupplier("Function 2", "Parent"));

            // execute
            Future<Void> result = future1.takeNewSnapshot(takeNewSnapshot)
                    .runAfterEitherAsync(future2, () -> assertContext("Parent"), contextUnawareThreadpool);

            // verify
            assertDoesNotThrow(() -> result.get());
        }
    }

    @Test
    public void testThenCompose() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("John")) {
            assertThat(supplyAsync(() -> {
                        String current = manager.getActiveContext().getValue();
                        manager.initializeNewContext("Travolta");
                        return current;
                    })
                            .thenCompose(value -> supplyAsync(
                                    () -> value + manager.getActiveContext().getValue()))
                            .get(),
                    is("JohnJohn"));
        }

        try (Context<String> ctx = manager.initializeNewContext("John")) {
            assertThat(supplyAsync(() -> {
                        String current = manager.getActiveContext().getValue();
                        manager.initializeNewContext("Travolta");
                        return current;
                    }, null, null, true)
                            .thenCompose(value -> supplyAsync(
                                    () -> value + manager.getActiveContext().getValue()))
                            .get(),
                    is("JohnTravolta"));
        }
    }

    @Test
    public void testThenComposeAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("John")) {
            assertThat(supplyAsync(() -> {
                        String current = manager.getActiveContext().getValue();
                        manager.initializeNewContext("Travolta");
                        return current;
                    })
                            .thenComposeAsync(value -> supplyAsync(
                                    () -> value + manager.getActiveContext().getValue()))
                            .get(),
                    is("JohnJohn"));
        }

        try (Context<String> ctx = manager.initializeNewContext("John")) {
            assertThat(supplyAsync(() -> {
                        String current = manager.getActiveContext().getValue();
                        manager.initializeNewContext("Travolta");
                        return current;
                    }, null, null, true)
                            .thenComposeAsync(value -> supplyAsync(
                                    () -> value + manager.getActiveContext().getValue()))
                            .get(),
                    is("JohnTravolta"));
        }
    }

    @Test
    public void testThenComposeAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("John")) {
            assertThat(supplyAsync(() -> {
                        String current = manager.getActiveContext().getValue();
                        manager.initializeNewContext("Travolta");
                        return current;
                    })
                            .thenComposeAsync(
                                    value -> completedFuture(value + manager.getActiveContext().getValue()),
                                    contextUnawareThreadpool)
                            .get(),
                    is("JohnJohn"));
        }

        try (Context<String> ctx = manager.initializeNewContext("John")) {
            assertThat(supplyAsync(() -> {
                        String current = manager.getActiveContext().getValue();
                        manager.initializeNewContext("Travolta");
                        return current;
                    }, null, null, true)
                            .thenComposeAsync(
                                    value -> completedFuture(value + manager.getActiveContext().getValue()),
                                    contextUnawareThreadpool)
                            .get(),
                    is("JohnTravolta"));
        }
    }

    @Test
    public void testTimingIssue55() throws ExecutionException, InterruptedException, TimeoutException {
        try (Context<String> ctx = manager.initializeNewContext("Vincent Vega")) {
            final CountDownLatch latch1 = new CountDownLatch(1), latch2 = new CountDownLatch(1);
            ContextAwareCompletableFuture<String> future1 = supplyAsync(() -> {
                String result = DummyContextManager.currentValue().orElse("NO VALUE");
                DummyContextManager.setCurrentValue("Jules Winnfield");
                waitFor(latch1);
                return result;
            }, null, null, true);
            ContextAwareCompletableFuture<String> future2 = future1.thenApplyAsync(value -> {
                String result = value + ", " + DummyContextManager.currentValue().orElse("NO VALUE");
                DummyContextManager.setCurrentValue("Marcellus Wallace");
                waitFor(latch2);
                return result;
            });
            Future<String> future3 = future2.thenApplyAsync(value ->
                    value + ", " + DummyContextManager.currentValue().orElse("NO VALUE"));

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
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Vincent Vega")));
        }
    }

    @Test
    public void testAllOf() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Vincent Vega");
        CompletableFuture<String> cf1 = new CompletableFuture<>();
        CompletableFuture<String> cf2 = new ContextAwareCompletableFuture<String>()
                .takeNewSnapshot()
                .thenApply(s -> {
                    manager.initializeNewContext("-" + s); // This context should be ignored
                    return s;
                });
        ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.allOf(cf1, cf2);
        manager.initializeNewContext("Jules Winnfield");

        ContextAwareCompletableFuture<String> result = future.thenApplyAsync(aVoid -> manager.getActiveContext().getValue());
        assertThat(result.isDone(), is(false));
        cf1.complete("Value 1");
        assertThat(result.isDone(), is(false));
        cf2.complete("Value 2");
        assertThat(result.get(), is("Vincent Vega"));
    }

    @Test
    public void testAllOfWithSpecificSnapshot() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Vincent Vega");
        final ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        manager.initializeNewContext("Marcellus Wallace");
        CompletableFuture<String> cf1 = new CompletableFuture<>();
        CompletableFuture<String> cf2 = new ContextAwareCompletableFuture<String>()
                .takeNewSnapshot()
                .thenApply(s -> {
                    manager.initializeNewContext("-" + s); // This context should be ignored
                    return s;
                });
        ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.allOf(snapshot, cf1, cf2);
        manager.initializeNewContext("Jules Winnfield");

        ContextAwareCompletableFuture<String> result = future.thenApplyAsync(aVoid -> manager.getActiveContext().getValue());
        assertThat(result.isDone(), is(false));
        cf1.complete("Value 1");
        assertThat(result.isDone(), is(false));
        cf2.complete("Value 2");
        assertThat(result.get(), is("Vincent Vega"));
    }

    @Test
    public void testAnyOf() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Vincent Vega");
        CompletableFuture<String> cf1 = new CompletableFuture<>();
        CompletableFuture<String> cf2 = new ContextAwareCompletableFuture<String>()
                .takeNewSnapshot()
                .thenApply(s -> {
                    manager.initializeNewContext("-" + s); // This context should be ignored
                    return s;
                });
        ContextAwareCompletableFuture<Object> future = ContextAwareCompletableFuture.anyOf(cf1, cf2);
        manager.initializeNewContext("Jules Winnfield");

        ContextAwareCompletableFuture<String> result = future
                .thenApplyAsync(s -> manager.getActiveContext().getValue());
        assertThat(result.isDone(), is(false));
        cf2.complete("Value 2");
        assertThat(result.get(), is("Vincent Vega"));
        assertThat(future.get(), is("Value 2"));
    }

    @Test
    public void testAnyOfWithSpecificSnapshot() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Vincent Vega");
        final ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        manager.initializeNewContext("Marcellus Wallace");
        CompletableFuture<String> cf1 = new CompletableFuture<>();
        CompletableFuture<String> cf2 = new ContextAwareCompletableFuture<String>()
                .takeNewSnapshot()
                .thenApply(s -> {
                    manager.initializeNewContext("-" + s); // This context should be ignored
                    return s;
                });
        ContextAwareCompletableFuture<Object> future = ContextAwareCompletableFuture.anyOf(snapshot, cf1, cf2);
        manager.initializeNewContext("Jules Winnfield");

        ContextAwareCompletableFuture<String> result = future
                .thenApplyAsync(s -> manager.getActiveContext().getValue());
        assertThat(result.isDone(), is(false));
        cf1.complete("Value 1");
        assertThat(result.get(), is("Vincent Vega"));
        assertThat(future.get(), is("Value 1"));
    }

    private static void waitFor(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted waiting for latch.", ie);
        }
    }
}
