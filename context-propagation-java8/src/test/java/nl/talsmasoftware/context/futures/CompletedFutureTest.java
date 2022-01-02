/*
 * Copyright 2016-2022 Talsma ICT
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.UnaryOperator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CompletedFutureTest {

    private static final DummyContextManager manager = new DummyContextManager();

    private static final UnaryOperator<String> addActiveContextValue = s -> s + ", " + manager.getActiveContext().getValue();

    @BeforeEach
    @AfterEach
    public void clearDummyContext() {
        DummyContextManager.clear();
    }

    @Test
    public void testCompletedFutureTakesNewSnapshot() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Mr. Blonde");
        final CompletableFuture<String> completed = ContextAwareCompletableFuture.completedFuture("Mr. Blue");
        manager.initializeNewContext("Mr. Brown");
        CompletableFuture<String> future = completed.thenApplyAsync(addActiveContextValue);

        assertThat(future.get(), is("Mr. Blue, Mr. Blonde"));
        assertThat(manager.getActiveContext().getValue(), is("Mr. Brown"));
    }

    @Test
    public void testCompletedFutureAppliesGivenSnapshot() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Mr. Blonde");
        final ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        manager.initializeNewContext("Mr. Brown");
        final CompletableFuture<String> completed = ContextAwareCompletableFuture.completedFuture("Mr. Blue", snapshot);
        manager.initializeNewContext("Mr. Orange");
        CompletableFuture<String> future = completed.thenApplyAsync(addActiveContextValue);

        assertThat(future.get(), is("Mr. Blue, Mr. Blonde"));
        assertThat(manager.getActiveContext().getValue(), is("Mr. Orange"));
    }

    @Test
    public void testCompletedStageTakesNewSnapshot() throws ExecutionException, InterruptedException {
        manager.initializeNewContext("Mr. Blonde");
        final CompletionStage<String> completed = ContextAwareCompletableFuture.completedStage("Mr. Blue");
        manager.initializeNewContext("Mr. Brown");
        CompletionStage<String> stage = completed.thenApplyAsync(addActiveContextValue);

        assertThat(stage.toCompletableFuture().get(), is("Mr. Blue, Mr. Blonde"));
        assertThat(manager.getActiveContext().getValue(), is("Mr. Brown"));
    }

}
