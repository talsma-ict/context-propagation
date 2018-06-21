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
import java.util.concurrent.CompletableFuture;
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
        manager.initializeNewContext("Jimmie");
        Future<Optional<String>> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Bonnie"))
                .thenApplyAsync(voidvalue -> DummyContextManager.currentValue());
        assertThat(future.get().get(), is("Jimmie")); // Bug 51: This should functionally be "Bonnie".
    }

    @Test
    public void testThenApplyAsync_executor() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Jimmie");
        Future<Optional<String>> future = ContextAwareCompletableFuture
                .runAsync(() -> manager.initializeNewContext("Bonnie"))
                .thenApplyAsync(voidvalue -> DummyContextManager.currentValue(), contextUnawareThreadpool);
        assertThat(future.get().get(), is("Jimmie")); // Bug 51: This should functionally be "Bonnie".
    }
}
