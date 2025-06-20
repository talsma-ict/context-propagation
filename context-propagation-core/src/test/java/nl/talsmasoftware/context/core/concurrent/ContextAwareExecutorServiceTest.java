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

import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.dummy.DummyContextManager;
import nl.talsmasoftware.context.dummy.ThrowingContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class ContextAwareExecutorServiceTest {
    static DummyContextManager dummyContextManager = new DummyContextManager();
    static ThrowingContextManager throwingContextManager = new ThrowingContextManager();

    static Callable<String> getDummyContext = dummyContextManager::getActiveContextValue;

    ContextAwareExecutorService executor;

    @BeforeEach
    void setupExecutor() {
        executor = ContextAwareExecutorService.wrap(Executors.newCachedThreadPool());
    }

    @AfterEach
    void tearDownExecutor() throws InterruptedException {
        executor.shutdown();
        assertThat(executor.awaitTermination(1, TimeUnit.MINUTES)).isTrue();
        executor = null;
    }

    @BeforeEach
    @AfterEach
    void clearActiveContexts() {
        ContextManager.clearAll();
    }

    @Test
    void testNoContext() throws ExecutionException, InterruptedException {
        Future<String> dummy = executor.submit(getDummyContext);
        assertThat(dummy.get()).isNull();
    }

    @Test
    void testContext() throws ExecutionException, InterruptedException {
        dummyContextManager.activate("The quick brown fox jumps over the lazy dog");
        Future<String> dummy = executor.submit(getDummyContext);
        dummyContextManager.activate("god yzal eht revo spmuj xof nworb kciuq ehT");
        assertThat(dummyContextManager.getActiveContextValue()).isEqualTo("god yzal eht revo spmuj xof nworb kciuq ehT");
        assertThat(dummy.get()).isEqualTo("The quick brown fox jumps over the lazy dog");
    }

    @Test
    void testCloseException() throws InterruptedException {
        throwingContextManager.activate("The quick brown fox jumps over the lazy dog");
        ThrowingContextManager.onClose = new IllegalStateException("Sometimes we stare so long at a door that is closing " +
                "that we see too late the one that is open. --Alexander Graham Bell");
        Future<String> dummy = executor.submit(getDummyContext);

        try {
            dummy.get();
            fail("Exception expected");
        } catch (ExecutionException expected) {
            assertThat(expected).cause().hasMessageContaining("a door that is closing");
        }
    }

    @Test
    void testCallException() throws InterruptedException {
        Future<String> dummy = executor.submit(() -> {
            throw new IllegalStateException("DOH!");
        });

        try {
            dummy.get();
            fail("Exception expected");
        } catch (ExecutionException expected) {
            assertThat(expected).cause().hasMessage("DOH!");
        }
    }

    @Test
    void testBothCallAndCloseException() throws InterruptedException {
        throwingContextManager.activate("The quick brown fox jumps over the lazy dog");
        ThrowingContextManager.onClose = new IllegalStateException("Sometimes we stare so long at a door that is closing " +
                "that we see too late the one that is open. --Alexander Graham Bell");
        Future<String> dummy = executor.submit(() -> {
            throw new IllegalStateException("DOH!");
        });

        try {
            dummy.get();
            fail("Exception expected");
        } catch (ExecutionException expected) {
            assertThat(expected).cause().hasMessage("DOH!");
        }
    }
}
