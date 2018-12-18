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
package nl.talsmasoftware.context.futures;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.DummyContextManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Sjoerd Talsma
 */
public class ContextAwareCompletableFutureTest {
    private static final DummyContextManager manager = new DummyContextManager();
    private static final ExecutorService contextUnawareThreadpool = Executors.newCachedThreadPool();

    @Before
    @After
    public void clearDummyContext() {
        DummyContextManager.clear();
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
        String expectedValue = "Vincent Vega";
        try (Context<String> ctx = manager.initializeNewContext(expectedValue)) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .supplyAsync(DummyContextManager::currentValue);
            assertThat(future.get().get(), is(expectedValue));
        }
    }

    @Test
    public void testSupplyAsync_executor() throws ExecutionException, InterruptedException {
        String expectedValue = "Marcellus Wallace";
        try (Context<String> ctx = manager.initializeNewContext(expectedValue)) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .supplyAsync(DummyContextManager::currentValue, contextUnawareThreadpool);
            assertThat(future.get().get(), is(expectedValue));
        }

    }

    @Test
    public void testSupplyAsync_executor_snapshot() throws ExecutionException, InterruptedException {
        String expectedValue = "Vincent Vega";
        try (Context<String> ctx = manager.initializeNewContext(expectedValue)) {
            ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
            manager.initializeNewContext("Jules Winnfield");
            assertThat(manager.getActiveContext().getValue(), is("Jules Winnfield"));

            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .supplyAsync(DummyContextManager::currentValue, contextUnawareThreadpool, snapshot);
            assertThat(future.get().get(), is(expectedValue));
        }
    }

    @Test
    public void testRunAsync() throws ExecutionException, InterruptedException {
        String expectedValue = "Mia Wallace";
        try (Context<String> ctx = manager.initializeNewContext(expectedValue)) {
            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> assertThat(DummyContextManager.currentValue(), is(Optional.of(expectedValue))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testRunAsync_executor() throws ExecutionException, InterruptedException {
        String expectedValue = "Jimmie";
        try (Context<String> ctx = manager.initializeNewContext(expectedValue)) {
            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(
                    () -> assertThat(DummyContextManager.currentValue(), is(Optional.of(expectedValue))),
                    contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testRunAsync_executor_snapshot() throws ExecutionException, InterruptedException {
        String expectedValue = "Pumpkin";
        try (Context<String> ctx = manager.initializeNewContext(expectedValue)) {
            ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
            manager.initializeNewContext("Honey Bunny");
            assertThat(manager.getActiveContext().getValue(), is("Honey Bunny"));

            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(
                    () -> assertThat(DummyContextManager.currentValue(), is(Optional.of(expectedValue))),
                    contextUnawareThreadpool,
                    snapshot);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenApply() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Jimmie")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Bonnie"))
                    .thenApply(voidvalue -> DummyContextManager.currentValue());
            assertThat(future.get().get(), is("Bonnie"));
        }
    }

    @Test
    public void testThenApplyAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Esmerelda Villalobos"))
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
            assertThat(future.get().get(), is("Zed"));
        }
    }

    @Test
    public void testThenApplyAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Jimmie")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Bonnie"))
                    .thenApplyAndTakeNewSnapshot(voidvalue -> {
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
                    .thenApplyAsyncAndTakeNewSnapshot(voidvalue -> {
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
                    .thenApplyAsyncAndTakeNewSnapshot(voidvalue -> {
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
                    .thenAccept(voidvalue -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Butch"))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenAcceptAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Fabienne"))
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
                            voidvalue -> assertThat(DummyContextManager.currentValue().get(), is("Winston Wolfe")),
                            contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenAcceptAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("The Gimp")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Butch"))
                    .thenAcceptAndTakeNewSnapshot(voidvalue -> {
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
                    .thenAcceptAsyncAndTakeNewSnapshot(voidvalue -> {
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
                    .thenAcceptAsyncAndTakeNewSnapshot(
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
                    .thenRun(() -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Jody"))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenRunAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Ringo")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Yolanda"))
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
                    .thenRunAndTakeNewSnapshot(() -> {
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
                    .thenRunAsyncAndTakeNewSnapshot(() -> {
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
                    .thenRunAsyncAndTakeNewSnapshot(
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
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Floyd"))
                    .whenComplete((voidValue, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Floyd"))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testWhenCompleteAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Zed")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Pipe hittin' niggers"))
                    .whenCompleteAsync((voidValue, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Pipe hittin' niggers"))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testWhenCompleteAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Floyd"))
                    .whenCompleteAsync(
                            (voidValue, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Floyd"))),
                            contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testHandle() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> {
                        manager.initializeNewContext("Trudy");
                        throw exception;
                    })
                    .handle((voidValue, throwable) -> DummyContextManager.currentValue());
            assertThat(future.get(), is(Optional.of("Trudy")));
        }
    }

    @Test
    public void testHandleAsync() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> {
                        manager.initializeNewContext("Trudy");
                        throw exception;
                    })
                    .handleAsync((voidValue, throwable) -> DummyContextManager.currentValue());
            assertThat(future.get(), is(Optional.of("Trudy")));
        }
    }

    @Test
    public void testHandleAsync_executor() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> {
                        manager.initializeNewContext("Trudy");
                        throw exception;
                    })
                    .handleAsync(
                            (voidValue, throwable) -> DummyContextManager.currentValue(),
                            contextUnawareThreadpool);
            assertThat(future.get(), is(Optional.of("Trudy")));
        }
    }

    @Test
    public void testExceptionally() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Gringo")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> {
                        manager.initializeNewContext("Jules Winnfield");
                        throw new RuntimeException("Bad Motherfucker");
                    })
                    .exceptionally(ex -> {
                        assertThat(DummyContextManager.currentValue(), is(Optional.of("Jules Winnfield")));
                        return null;
                    });
            future.get();
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
            assertThat(future.get(), is(Optional.of("Marvin")));
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Brett")));
        }
    }

    @Test
    public void testThenCombineAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Marcellus Wallace")) {
            Future<String> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Vincent Vega"))
                    .thenCombineAndTakeNewSnapshot(
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
                    .thenCombineAsyncAndTakeNewSnapshot(
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
                    .thenCombineAsyncAndTakeNewSnapshot(
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
                    .runAsync(() -> manager.initializeNewContext("Quentin").getValue())
                    .thenAcceptBoth(completedFuture("Tarantino"),
                            (Void voidA, String stringB) ->
                                    assertThat(manager.getActiveContext().getValue() + stringB,
                                            is("QuentinTarantino")))
                    .get();
        }
    }

    @Test
    public void testThenAcceptBothAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Quentin").getValue())
                    .thenAcceptBothAsync(completedFuture("Tarantino"),
                            (Void voidA, String stringB) ->
                                    assertThat(manager.getActiveContext().getValue() + stringB,
                                            is("QuentinTarantino")))
                    .get();
        }
    }

    @Test
    public void testThenAcceptBothAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Quentin").getValue())
                    .thenAcceptBothAsync(completedFuture("Tarantino"),
                            (Void voidA, String stringB) ->
                                    assertThat(manager.getActiveContext().getValue() + stringB,
                                            is("QuentinTarantino")),
                            contextUnawareThreadpool)
                    .get();
        }
    }

    @Test
    public void testThenAcceptBothAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Quentin").getValue())
                    .thenAcceptBothAndTakeNewSnapshot(completedFuture("Tarantino"),
                            (Void voidA, String stringB) -> {
                                String val = manager.getActiveContext().getValue() + stringB;
                                assertThat(val, is("QuentinTarantino"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-QuentinTarantino")))
                    .get();
        }
    }

    @Test
    public void testThenAcceptBothAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Quentin").getValue())
                    .thenAcceptBothAsyncAndTakeNewSnapshot(completedFuture("Tarantino"),
                            (Void voidA, String stringB) -> {
                                String val = manager.getActiveContext().getValue() + stringB;
                                assertThat(val, is("QuentinTarantino"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-QuentinTarantino")))
                    .get();
        }
    }

    @Test
    public void testThenAcceptBothAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Pulp Fiction")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Quentin").getValue())
                    .thenAcceptBothAsyncAndTakeNewSnapshot(completedFuture("Tarantino"),
                            (Void voidA, String stringB) -> {
                                String val = manager.getActiveContext().getValue() + stringB;
                                assertThat(val, is("QuentinTarantino"));
                                manager.initializeNewContext("-" + val);
                            },
                            contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-QuentinTarantino")))
                    .get();
        }
    }

    @Test
    public void testRunAfterBoth() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"))
                    .runAfterBoth(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> assertThat(manager.getActiveContext().getValue(), is("Ketchup")))
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
                            () -> assertThat(manager.getActiveContext().getValue(), is("Ketchup")))
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
                            () -> assertThat(manager.getActiveContext().getValue(), is("Ketchup")),
                            contextUnawareThreadpool)
                    .get();
        }
    }

    @Test
    public void testRunAfterBothAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"))
                    .runAfterBothAndTakeNewSnapshot(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("Ketchup"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-Ketchup")))
                    .get();
        }
    }

    @Test
    public void testRunAfterBothAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"))
                    .runAfterBothAsyncAndTakeNewSnapshot(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("Ketchup"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-Ketchup")))
                    .get();
        }
    }

    @Test
    public void testRunAfterBothAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"))
                    .runAfterBothAsyncAndTakeNewSnapshot(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("Ketchup"));
                                manager.initializeNewContext("-" + val);
                            },
                            contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-Ketchup")))
                    .get();
        }
    }

    @Test
    public void testApplyToEither() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Quarterpounder with Cheese")) {
            assertThat(ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Royale with Cheese"))
                    .applyToEither(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Metric system")),
                            (voidValue) -> manager.getActiveContext().getValue())
                    .get(), isOneOf("Quarterpounder with Cheese", "Royale with Cheese"));
        }
    }

    @Test
    public void testApplyToEitherAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Quarterpounder with Cheese")) {
            assertThat(ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Royale with Cheese"))
                    .applyToEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Metric system")),
                            (voidValue) -> manager.getActiveContext().getValue())
                    .get(), isOneOf("Quarterpounder with Cheese", "Royale with Cheese"));
        }
    }

    @Test
    public void testApplyToEitherAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Quarterpounder with Cheese")) {
            assertThat(ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Royale with Cheese"))
                    .applyToEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Metric system")),
                            voidValue -> manager.getActiveContext().getValue(),
                            contextUnawareThreadpool)
                    .get(), isOneOf("Quarterpounder with Cheese", "Royale with Cheese"));
        }
    }

    @Test
    public void testAcceptEither() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Hash bar")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Hash is legal there"))
                    .acceptEither(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Smoke at home or certain designated places")),
                            voidValue -> assertThat(manager.getActiveContext().getValue(),
                                    isOneOf("Hash bar", "Hash is legal there")))
                    .get();
        }
    }

    @Test
    public void testAcceptEitherAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Hash bar")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Hash is legal there"))
                    .acceptEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Smoke at home or certain designated places")),
                            voidValue -> assertThat(manager.getActiveContext().getValue(),
                                    isOneOf("Hash bar", "Hash is legal there")))
                    .get();
        }
    }

    @Test
    public void testAcceptEitherAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Hash bar")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Hash is legal there"))
                    .acceptEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Smoke at home or certain designated places")),
                            voidValue -> assertThat(manager.getActiveContext().getValue(),
                                    isOneOf("Hash bar", "Hash is legal there")),
                            contextUnawareThreadpool)
                    .get();
        }
    }

    @Test
    public void testRunAfterEither() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Movie theater")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Glass of beer"))
                    .runAfterEither(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Paper cup")),
                            () -> assertThat(manager.getActiveContext().getValue(),
                                    isOneOf("Movie theater", "Glass of beer")))
                    .get();
        }
    }

    @Test
    public void testRunAfterEitherAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Movie theater")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Glass of beer"))
                    .runAfterEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Paper cup")),
                            () -> assertThat(manager.getActiveContext().getValue(),
                                    isOneOf("Movie theater", "Glass of beer")))
                    .get();
        }
    }

    @Test
    public void testRunAfterEitherAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Movie theater")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Glass of beer"))
                    .runAfterEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Paper cup")),
                            () -> assertThat(manager.getActiveContext().getValue(),
                                    isOneOf("Movie theater", "Glass of beer")),
                            contextUnawareThreadpool)
                    .get();
        }
    }

    @Test
    public void testThenCompose() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("John")) {
            assertThat(ContextAwareCompletableFuture
                            .supplyAsync(() -> {
                                String current = manager.getActiveContext().getValue();
                                manager.initializeNewContext("Travolta");
                                return current;
                            })
                            .thenCompose(value -> ContextAwareCompletableFuture.supplyAsync(
                                    () -> value + manager.getActiveContext().getValue()))
                            .get(),
                    is("JohnTravolta"));
        }
    }

    @Test
    public void testThenComposeAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("John")) {
            assertThat(ContextAwareCompletableFuture
                            .supplyAsync(() -> {
                                String current = manager.getActiveContext().getValue();
                                manager.initializeNewContext("Travolta");
                                return current;
                            })
                            .thenComposeAsync(value -> ContextAwareCompletableFuture.supplyAsync(
                                    () -> value + manager.getActiveContext().getValue()))
                            .get(),
                    is("JohnTravolta"));
        }
    }

    @Test
    public void testThenComposeAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("John")) {
            assertThat(ContextAwareCompletableFuture
                            .supplyAsync(() -> {
                                String current = manager.getActiveContext().getValue();
                                manager.initializeNewContext("Travolta");
                                return current;
                            })
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
            ContextAwareCompletableFuture<String> future1 = ContextAwareCompletableFuture
                    .supplyAsync(() -> {
                        String result = DummyContextManager.currentValue().orElse("NO VALUE");
                        DummyContextManager.setCurrentValue("Jules Winnfield");
                        waitFor(latch1);
                        return result;
                    });
            ContextAwareCompletableFuture<String> future2 = future1.thenApplyAsyncAndTakeNewSnapshot(value -> {
                String result = value + ", " + DummyContextManager.currentValue().orElse("NO VALUE");
                DummyContextManager.setCurrentValue("Marcellus Wallace");
                waitFor(latch2);
                return result;
            });
            Future<String> future3 = future2.thenApplyAsyncAndTakeNewSnapshot(value ->
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

    private static void waitFor(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted waiting for latch.", ie);
        }
    }
}
