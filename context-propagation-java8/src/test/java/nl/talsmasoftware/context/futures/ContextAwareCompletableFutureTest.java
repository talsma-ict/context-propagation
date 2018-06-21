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

import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Sjoerd Talsma
 */
public class ContextAwareCompletableFutureTest {
    private static final DummyContextManager manager = new DummyContextManager();
    private static final ExecutorService contextUnawareThreadpool = Executors.newCachedThreadPool();

    @Test
    public void testSupplyAsync() throws ExecutionException, InterruptedException {
        String expectedValue = "Vincent Vega";
        manager.initializeNewContext(expectedValue);
        Future<Optional<String>> future = ContextAwareCompletableFuture
                .supplyAsync(DummyContextManager::currentValue);
        assertThat(future.get().get(), is(expectedValue));
    }

    @Test
    public void testSupplyAsync_executor() throws ExecutionException, InterruptedException {
        String expectedValue = "Marcellus Wallace";
        manager.initializeNewContext(expectedValue);
        Future<Optional<String>> future = ContextAwareCompletableFuture
                .supplyAsync(DummyContextManager::currentValue, contextUnawareThreadpool);
        assertThat(future.get().get(), is(expectedValue));

    }

    @Test
    public void testSupplyAsync_executor_snapshot() throws ExecutionException, InterruptedException {
        String expectedValue = "Vincent Vega";
        manager.initializeNewContext(expectedValue);
        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        manager.initializeNewContext("Jules Winnfield");
        assertThat(manager.getActiveContext().getValue(), is("Jules Winnfield"));

        Future<Optional<String>> future = ContextAwareCompletableFuture
                .supplyAsync(DummyContextManager::currentValue, contextUnawareThreadpool, snapshot);
        assertThat(future.get().get(), is(expectedValue));
    }

    @Test
    public void testRunAsync() throws ExecutionException, InterruptedException {
        String expectedValue = "Mia Wallace";
        manager.initializeNewContext(expectedValue);
        ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture
                .runAsync(() -> assertThat(DummyContextManager.currentValue(), is(Optional.of(expectedValue))));
        future.get(); // trigger asynchronous assertion
    }

    @Test
    public void testRunAsync_executor() throws ExecutionException, InterruptedException {
        String expectedValue = "Jimmie";
        manager.initializeNewContext(expectedValue);
        ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(
                () -> assertThat(DummyContextManager.currentValue(), is(Optional.of(expectedValue))),
                contextUnawareThreadpool);
        future.get(); // trigger asynchronous assertion
    }

    @Test
    public void testRunAsync_executor_snapshot() throws ExecutionException, InterruptedException {
        String expectedValue = "Pumpkin";
        manager.initializeNewContext(expectedValue);
        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        manager.initializeNewContext("Honey Bunny");
        assertThat(manager.getActiveContext().getValue(), is("Honey Bunny"));

        ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(
                () -> assertThat(DummyContextManager.currentValue(), is(Optional.of(expectedValue))),
                contextUnawareThreadpool,
                snapshot);
        future.get(); // trigger asynchronous assertion
    }

    @Test
    public void testThenApply() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Jimmie");
        Future<Optional<String>> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Bonnie"))
                .thenApply(voidvalue -> DummyContextManager.currentValue());
        assertThat(future.get().get(), is("Jimmie")); // Bug 51: This should functionally be "Bonnie".
    }

    @Test
    public void testThenApplyAsync() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Butch");
        Future<Optional<String>> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Esmerelda Villalobos"))
                .thenApplyAsync(voidvalue -> DummyContextManager.currentValue());
        assertThat(future.get().get(), is("Butch")); // Bug 51: This should functionally be "Esmerelda Villalobos".
    }

    @Test
    public void testThenApplyAsync_executor() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Maynard");
        Future<Optional<String>> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Zed"))
                .thenApplyAsync(voidvalue -> DummyContextManager.currentValue(), contextUnawareThreadpool);
        assertThat(future.get().get(), is("Maynard")); // Bug 51: This should functionally be "Zed".
    }

    @Test
    public void testThenAccept() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("The Gimp");
        Future<Void> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Butch"))
                .thenAccept(voidvalue -> assertThat(DummyContextManager.currentValue(), is(Optional.of("The Gimp"))));
        future.get(); // trigger asynchronous assertion // Bug 51: This should functionally be "Butch".
    }

    @Test
    public void testThenAcceptAsync() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Butch");
        Future<Void> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Fabienne"))
                .thenAcceptAsync(voidvalue -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Butch"))));
        future.get(); // trigger asynchronous assertion // Bug 51: This should functionally be "Fabienne".
    }

    @Test
    public void testThenAcceptAsync_executor() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Marvin");
        Future<Void> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Winston Wolfe"))
                .thenAcceptAsync(                       // Bug 51: This should functionally be "Winston Wolfe".
                        voidvalue -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Marvin"))),
                        contextUnawareThreadpool);
        future.get(); // trigger asynchronous assertion
    }

    @Test
    public void testThenRun() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Lance");
        Future<Void> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Jody"))
                .thenRun(() -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Lance"))));
        future.get(); // trigger asynchronous assertion     // Bug 51: This should functionally be "Jody".
    }

    @Test
    public void testThenRunAsync() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Ringo");
        Future<Void> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Yolanda"))
                .thenRunAsync(() -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Ringo"))));
        future.get(); // trigger asynchronous assertion     // Bug 51: This should functionally be "Yolanda".
    }

    @Test
    public void testThenRunAsync_executor() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Capt. Koons");
        Future<Void> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Butch"))
                .thenRunAsync(
                        () -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Capt. Koons"))),
                        contextUnawareThreadpool);
        future.get(); // trigger asynchronous assertion     // Bug 51: This should functionally be "Butch".
    }

    @Test
    public void testWhenComplete() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Butch");
        Future<Void> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Floyd"))
                .whenComplete((voidValue, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Butch"))));
        future.get(); // trigger asynchronous assertion                 // Bug 51: This should functionally be "Floyd".
    }

    @Test
    public void testWhenCompleteAsync() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Zed");
        Future<Void> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Pipe hittin' niggers"))
                .whenCompleteAsync((voidValue, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Zed"))));
        future.get(); // trigger asynchronous assertion       // Bug 51: This should functionally be "Pipe hittin' niggers".
    }

    @Test
    public void testWhenCompleteAsync_executor() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Butch");
        Future<Void> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Floyd"))
                .whenCompleteAsync(
                        (voidValue, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Butch"))),
                        contextUnawareThreadpool);
        future.get(); // trigger asynchronous assertion                 // Bug 51: This should functionally be "Floyd".
    }

    @Test
    public void testHandle() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        manager.initializeNewContext("Jody");
        Future<Optional<String>> future = ContextAwareCompletableFuture
                .runAsync(() -> {
                    manager.initializeNewContext("Trudy");
                    throw exception;
                })
                .handle((voidValue, throwable) -> DummyContextManager.currentValue());
        assertThat(future.get(), is(Optional.of("Jody"))); // Bug 51: This should functionally be "Trudy".
    }

    @Test
    public void testHandleAsync() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        manager.initializeNewContext("Jody");
        Future<Optional<String>> future = ContextAwareCompletableFuture
                .runAsync(() -> {
                    manager.initializeNewContext("Trudy");
                    throw exception;
                })
                .handleAsync((voidValue, throwable) -> DummyContextManager.currentValue());
        assertThat(future.get(), is(Optional.of("Jody"))); // Bug 51: This should functionally be "Trudy".
    }

    @Test
    public void testHandleAsync_executor() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        manager.initializeNewContext("Jody");
        Future<Optional<String>> future = ContextAwareCompletableFuture
                .runAsync(() -> {
                    manager.initializeNewContext("Trudy");
                    throw exception;
                })
                .handleAsync(
                        (voidValue, throwable) -> DummyContextManager.currentValue(),
                        contextUnawareThreadpool);
        assertThat(future.get(), is(Optional.of("Jody"))); // Bug 51: This should functionally be "Trudy".
    }

    @Test
    public void testExceptionally() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Gringo");
        Future<Void> future = ContextAwareCompletableFuture
                .runAsync(() -> {
                    manager.initializeNewContext("Jules Winnfield");
                    throw new RuntimeException("Bad Motherfucker");
                })
                .exceptionally(ex -> {
                    assertThat(DummyContextManager.currentValue(), is(Optional.of("Gringo")));
                    return null;            // Bug 51: This should functionally be "Jules Winnfield".
                });
        future.get();
    }

    /**
     * This test for 'combine' is interesting.
     * <p>
     * When combining two processes, which context should be available in the combination function?
     * Possibly the choice-of-least-surprise may be to use the original context snapshot.
     */
    @Test
    public void testThenCombine() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Marcellus Wallace");
        Future<Optional<String>> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Vincent Vega"))
                .thenCombine(
                        ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Jules Winnfield")),
                        (voidA, voidB) -> DummyContextManager.currentValue());
        assertThat(future.get(), is(Optional.of("Marcellus Wallace")));
    }

    @Test
    public void testThenCombineAsync() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Brett");
        Future<Optional<String>> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Marvin"))
                .thenCombineAsync(
                        ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Flock of Seagulls")),
                        (voidA, voidB) -> DummyContextManager.currentValue());
        assertThat(future.get(), is(Optional.of("Brett")));
    }
}
