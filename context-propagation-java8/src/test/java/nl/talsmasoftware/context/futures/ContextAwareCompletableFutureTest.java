/*
 * Copyright 2016-2019 Talsma ICT
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
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.hamcrest.Matchers.startsWith;

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
        try (Context<String> ctx = manager.initializeNewContext("Vincent Vega")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .supplyAsync(DummyContextManager::currentValue);
            assertThat(future.get().get(), is("Vincent Vega"));
        }
    }

    @Test
    public void testSupplyAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Marcellus Wallace")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .supplyAsync(DummyContextManager::currentValue, contextUnawareThreadpool);
            assertThat(future.get().get(), is("Marcellus Wallace"));
        }
    }

    @Test
    public void testSupplyAsync_executor_snapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Vincent Vega")) {
            ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
            manager.initializeNewContext("Jules Winnfield");
            assertThat(manager.getActiveContext().getValue(), is("Jules Winnfield"));

            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .supplyAsync(DummyContextManager::currentValue, contextUnawareThreadpool, snapshot);
            assertThat(future.get().get(), is("Vincent Vega"));
        }
    }

    @Test
    public void testSupplyAsync_executor_snapshot_takeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Vincent Vega")) {
            ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
            manager.initializeNewContext("Jules Winnfield");
            assertThat(manager.getActiveContext().getValue(), is("Jules Winnfield"));

            ContextAwareCompletableFuture<Optional<String>> future = ContextAwareCompletableFuture
                    .supplyAsync(() -> {
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
            assertThat(manager.getActiveContext().getValue(), is("Honey Bunny"));

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
            assertThat(manager.getActiveContext().getValue(), is("Honey Bunny"));

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
                            .whenComplete((result, throwable) -> assertThat(manager.getActiveContext().getValue(), is("-Jody")))
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
                            .whenComplete((result, throwable) -> assertThat(manager.getActiveContext().getValue(), is("-Trudy")))
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
                            .whenComplete((result, throwable) -> assertThat(manager.getActiveContext().getValue(), is("-Jody")))
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
                            .whenComplete((result, throwable) -> assertThat(manager.getActiveContext().getValue(), is("-Trudy")))
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
                            .whenComplete((result, throwable) -> assertThat(manager.getActiveContext().getValue(), is("-Jody")))
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
                            .whenComplete((result, throwable) -> assertThat(manager.getActiveContext().getValue(), is("-Trudy")))
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
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-Gringo")))
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
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-Jules Winnfield")))
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
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-Pulp Fiction by Tarantino")))
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
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-Quentin by Tarantino")))
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
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-Pulp Fiction by Tarantino")))
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
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-QuentinTarantino")))
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
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-Pulp Fiction by Tarantino")))
                    .get();
            assertThat(manager.getActiveContext().getValue(), is("Pulp Fiction"));
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
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-QuentinTarantino")))
                    .get();
            assertThat(manager.getActiveContext().getValue(), is("Pulp Fiction"));
        }
    }

    @Test
    public void testRunAfterBoth() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"))
                    .runAfterBoth(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> assertThat(manager.getActiveContext().getValue(), is("French Fries")))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"), null, null, true)
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
                            () -> assertThat(manager.getActiveContext().getValue(), is("French Fries")))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"), null, null, true)
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
                            () -> assertThat(manager.getActiveContext().getValue(), is("French Fries")),
                            contextUnawareThreadpool)
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("French Fries")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Ketchup"), null, null, true)
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
                    .takeNewSnapshot()
                    .runAfterBoth(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Mayonaise")),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("French Fries"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-French Fries")))
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
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-Ketchup")))
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
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-French Fries")))
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
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-Ketchup")))
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
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-French Fries")))
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
                    .get(), is("Quarterpounder with Cheese"));
        }

        try (Context<String> ctx = manager.initializeNewContext("Quarterpounder with Cheese")) {
            assertThat(ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Royale with Cheese"), null, null, true)
                    .applyToEither(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Metric system")),
                            (voidValue) -> manager.getActiveContext().getValue())
                    .get(), oneOf("Quarterpounder with Cheese", "Royale with Cheese"));
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
                    .get(), is("Quarterpounder with Cheese"));
        }

        try (Context<String> ctx = manager.initializeNewContext("Quarterpounder with Cheese")) {
            assertThat(ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Royale with Cheese"), null, null, true)
                    .applyToEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Metric system")),
                            (voidValue) -> manager.getActiveContext().getValue())
                    .get(), oneOf("Quarterpounder with Cheese", "Royale with Cheese"));
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
                    .get(), is("Quarterpounder with Cheese"));
        }

        try (Context<String> ctx = manager.initializeNewContext("Quarterpounder with Cheese")) {
            assertThat(ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Royale with Cheese"), null, null, true)
                    .applyToEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Metric system")),
                            voidValue -> manager.getActiveContext().getValue(),
                            contextUnawareThreadpool)
                    .get(), oneOf("Quarterpounder with Cheese", "Royale with Cheese"));
        }
    }

    @Test
    public void testApplyToEitherAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Quarterpounder with Cheese")) {
            assertThat(ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Royale with Cheese"))
                    .takeNewSnapshot()
                    .applyToEither(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Metric system")),
                            (voidValue) -> {
                                String val = manager.getActiveContext().getValue();
                                manager.initializeNewContext("-" + val);
                                return val;
                            })
                    .whenComplete((result, exception) -> assertThat(manager.getActiveContext().getValue(), startsWith("-")))
                    .get(), is("Quarterpounder with Cheese"));
        }

        try (Context<String> ctx = manager.initializeNewContext("Quarterpounder with Cheese")) {
            assertThat(ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Royale with Cheese"), null, null, true)
                    .applyToEither(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Metric system"), null, null, true),
                            (voidValue) -> {
                                String val = manager.getActiveContext().getValue();
                                manager.initializeNewContext("-" + val);
                                return val;
                            })
                    .whenComplete((result, exception) -> assertThat(manager.getActiveContext().getValue(), startsWith("-")))
                    .get(), oneOf("Quarterpounder with Cheese", "Royale with Cheese"));
        }
    }

    @Test
    public void testApplyToEitherAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Quarterpounder with Cheese")) {
            assertThat(ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Royale with Cheese"))
                    .takeNewSnapshot()
                    .applyToEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Metric system")),
                            (voidValue) -> {
                                String val = manager.getActiveContext().getValue();
                                manager.initializeNewContext("-" + val);
                                return val;
                            })
                    .whenComplete((result, exception) -> assertThat(manager.getActiveContext().getValue(), startsWith("-")))
                    .get(), is("Quarterpounder with Cheese"));
        }
    }

    @Test
    public void testApplyToEitherAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Quarterpounder with Cheese")) {
            assertThat(ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Royale with Cheese"))
                    .takeNewSnapshot()
                    .applyToEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Metric system")),
                            (voidValue) -> {
                                String val = manager.getActiveContext().getValue();
                                manager.initializeNewContext("-" + val);
                                return val;
                            },
                            contextUnawareThreadpool)
                    .whenComplete((result, exception) -> assertThat(manager.getActiveContext().getValue(), startsWith("-")))
                    .get(), is("Quarterpounder with Cheese"));
        }

        try (Context<String> ctx = manager.initializeNewContext("Quarterpounder with Cheese")) {
            assertThat(ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Royale with Cheese"), null, null, true)
                    .applyToEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Metric system"), null, null, true),
                            (voidValue) -> {
                                String val = manager.getActiveContext().getValue();
                                manager.initializeNewContext("-" + val);
                                return val;
                            },
                            contextUnawareThreadpool)
                    .whenComplete((result, exception) -> assertThat(manager.getActiveContext().getValue(), startsWith("-")))
                    .get(), oneOf("Quarterpounder with Cheese", "Royale with Cheese"));
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
                                    is("Hash bar")))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("Hash bar")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Hash is legal there"), null, null, true)
                    .acceptEither(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Smoke at home or certain designated places"), null, null, true),
                            voidValue -> assertThat(manager.getActiveContext().getValue(),
                                    oneOf("Hash bar", "Hash is legal there")))
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
                                    is("Hash bar")))
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
                                    is("Hash bar")),
                            contextUnawareThreadpool)
                    .get();
        }
    }

    @Test
    public void testAcceptEitherAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Hash bar")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Hash is legal there"))
                    .takeNewSnapshot()
                    .acceptEither(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Smoke at home or certain designated places")),
                            voidValue -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("Hash bar"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-Hash bar")))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("Hash bar")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Hash is legal there"), null, null, true)
                    .takeNewSnapshot()
                    .acceptEither(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Smoke at home or certain designated places"), null, null, true),
                            voidValue -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, oneOf("Hash bar", "Hash is legal there"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), startsWith("-Hash")))
                    .get();
        }
    }

    @Test
    public void testAcceptEitherAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Hash bar")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Hash is legal there"))
                    .takeNewSnapshot()
                    .acceptEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Smoke at home or certain designated places")),
                            voidValue -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("Hash bar"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), startsWith("-Hash bar")))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("Hash bar")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Hash is legal there"), null, null, true)
                    .takeNewSnapshot()
                    .acceptEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Smoke at home or certain designated places"), null, null, true),
                            voidValue -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, oneOf("Hash bar", "Hash is legal there"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), startsWith("-Hash")))
                    .get();
        }
    }

    @Test
    public void testAcceptEitherAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Hash bar")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Hash is legal there"))
                    .takeNewSnapshot()
                    .acceptEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Smoke at home or certain designated places")),
                            voidValue -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("Hash bar"));
                                manager.initializeNewContext("-" + val);
                            },
                            contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), startsWith("-Hash bar")))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("Hash bar")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Hash is legal there"), null, null, true)
                    .takeNewSnapshot()
                    .acceptEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Smoke at home or certain designated places"), null, null, true),
                            voidValue -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, oneOf("Hash bar", "Hash is legal there"));
                                manager.initializeNewContext("-" + val);
                            },
                            contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), startsWith("-Hash")))
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
                                    is("Movie theater")))
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
                                    is("Movie theater")))
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
                                    is("Movie theater")),
                            contextUnawareThreadpool)
                    .get();
        }
    }

    @Test
    public void testRunAfterEitherAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Movie theater")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Glass of beer"))
                    .takeNewSnapshot()
                    .runAfterEither(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Paper cup")),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("Movie theater"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), startsWith("-")))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("Movie theater")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Glass of beer"), null, null, true)
                    .takeNewSnapshot()
                    .runAfterEither(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Paper cup"), null, null, true),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, oneOf("Movie theater", "Glass of beer"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), startsWith("-")))
                    .get();
        }
    }

    @Test
    public void testRunAfterEitherAsyncAndTakeNewSnapshot() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Movie theater")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Glass of beer"))
                    .takeNewSnapshot()
                    .runAfterEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Paper cup")),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("Movie theater"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-Movie theater")))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("Movie theater")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Glass of beer"), null, null, true)
                    .takeNewSnapshot()
                    .runAfterEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Paper cup"), null, null, true),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, oneOf("Movie theater", "Glass of beer"));
                                manager.initializeNewContext("-" + val);
                            })
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), startsWith("-")))
                    .get();
        }
    }

    @Test
    public void testRunAfterEitherAsyncAndTakeNewSnapshot_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Movie theater")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Glass of beer"))
                    .takeNewSnapshot()
                    .runAfterEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Paper cup")),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, is("Movie theater"));
                                manager.initializeNewContext("-" + val);
                            },
                            contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), is("-Movie theater")))
                    .get();
        }

        try (Context<String> ctx = manager.initializeNewContext("Movie theater")) {
            ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Glass of beer"), null, null, true)
                    .takeNewSnapshot()
                    .runAfterEitherAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Paper cup"), null, null, true),
                            () -> {
                                String val = manager.getActiveContext().getValue();
                                assertThat(val, oneOf("Movie theater", "Glass of beer"));
                                manager.initializeNewContext("-" + val);
                            },
                            contextUnawareThreadpool)
                    .thenAccept(aVoid -> assertThat(manager.getActiveContext().getValue(), startsWith("-")))
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
                    is("JohnJohn"));
        }

        try (Context<String> ctx = manager.initializeNewContext("John")) {
            assertThat(ContextAwareCompletableFuture
                            .supplyAsync(() -> {
                                String current = manager.getActiveContext().getValue();
                                manager.initializeNewContext("Travolta");
                                return current;
                            }, null, null, true)
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
                    is("JohnJohn"));
        }

        try (Context<String> ctx = manager.initializeNewContext("John")) {
            assertThat(ContextAwareCompletableFuture
                            .supplyAsync(() -> {
                                String current = manager.getActiveContext().getValue();
                                manager.initializeNewContext("Travolta");
                                return current;
                            }, null, null, true)
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
                    is("JohnJohn"));
        }

        try (Context<String> ctx = manager.initializeNewContext("John")) {
            assertThat(ContextAwareCompletableFuture
                            .supplyAsync(() -> {
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
            ContextAwareCompletableFuture<String> future1 = ContextAwareCompletableFuture
                    .supplyAsync(() -> {
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

    private static void waitFor(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted waiting for latch.", ie);
        }
    }
}
