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

import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.dummy.DummyContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

class CompletedFutureTest {

    private static final DummyContextManager manager = new DummyContextManager();

    private static final UnaryOperator<String> addActiveContextValue = s -> s + ", " + manager.getActiveContextValue();

    @BeforeEach
    @AfterEach
    void clearDummyContext() {
        DummyContextManager.clearAllContexts();
    }

    @Test
    void testCompletedFutureTakesNewSnapshot() throws ExecutionException, InterruptedException {
        manager.activate("Mr. Blonde");
        final CompletableFuture<String> completed = ContextAwareCompletableFuture.completedFuture("Mr. Blue");
        manager.activate("Mr. Brown");
        CompletableFuture<String> future = completed.thenApplyAsync(addActiveContextValue);

        assertThat(future.get()).isEqualTo("Mr. Blue, Mr. Blonde");
        assertThat(manager.getActiveContextValue()).isEqualTo("Mr. Brown");
    }

    @Test
    void testCompletedFutureAppliesGivenSnapshot() throws ExecutionException, InterruptedException {
        manager.activate("Mr. Blonde");
        final ContextSnapshot snapshot = ContextSnapshot.capture();
        manager.activate("Mr. Brown");
        final CompletableFuture<String> completed = ContextAwareCompletableFuture.completedFuture("Mr. Blue", snapshot);
        manager.activate("Mr. Orange");
        CompletableFuture<String> future = completed.thenApplyAsync(addActiveContextValue);

        assertThat(future.get()).isEqualTo("Mr. Blue, Mr. Blonde");
        assertThat(manager.getActiveContextValue()).isEqualTo("Mr. Orange");
    }

    @Test
    void testCompletedStageTakesNewSnapshot() throws ExecutionException, InterruptedException {
        manager.activate("Mr. Blonde");
        final CompletionStage<String> completed = ContextAwareCompletableFuture.completedStage("Mr. Blue");
        manager.activate("Mr. Brown");
        CompletionStage<String> stage = completed.thenApplyAsync(addActiveContextValue);

        assertThat(stage.toCompletableFuture().get()).isEqualTo("Mr. Blue, Mr. Blonde");
        assertThat(manager.getActiveContextValue()).isEqualTo("Mr. Brown");
    }

}
