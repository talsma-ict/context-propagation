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
package nl.talsmasoftware.context.core.concurrent;

import nl.talsmasoftware.context.core.ContextManagers;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;

public class ContextAwareExecutorServiceTest {
    private static DummyContextManager dummyContextManager = new DummyContextManager();
    private static ThrowingContextManager throwingContextManager = new ThrowingContextManager();

    private static Callable<String> getDummyContext = dummyContextManager::getActiveContextValue;

    private ContextAwareExecutorService executor;

    @BeforeEach
    public void setupExecutor() {
        executor = new ContextAwareExecutorService(Executors.newCachedThreadPool());
    }

    @AfterEach
    public void tearDownExecutor() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        executor = null;
    }

    @BeforeEach
    @AfterEach
    public void clearActiveContexts() {
        ContextManagers.clearActiveContexts();
    }

    @Test
    public void testNoContext() throws ExecutionException, InterruptedException {
        Future<String> dummy = executor.submit(getDummyContext);
        assertThat(dummy.get(), is(nullValue()));
    }

    @Test
    public void testContext() throws ExecutionException, InterruptedException {
        dummyContextManager.initializeNewContext("The quick brown fox jumps over the lazy dog");
        Future<String> dummy = executor.submit(getDummyContext);
        dummyContextManager.initializeNewContext("god yzal eht revo spmuj xof nworb kciuq ehT");
        assertThat(dummyContextManager.getActiveContextValue(), is("god yzal eht revo spmuj xof nworb kciuq ehT"));
        assertThat(dummy.get(), is("The quick brown fox jumps over the lazy dog"));
    }

    @Test
    public void testCloseException() throws InterruptedException {
        throwingContextManager.initializeNewContext("The quick brown fox jumps over the lazy dog");
        ThrowingContextManager.onClose = new IllegalStateException("Sometimes we stare so long at a door that is closing " +
                "that we see too late the one that is open. --Alexander Graham Bell");
        Future<String> dummy = executor.submit(getDummyContext);

        try {
            dummy.get();
            fail("Exception expected");
        } catch (ExecutionException expected) {
            assertThat(expected.getCause().getMessage(), containsString("a door that is closing"));
        }
    }

    @Test
    public void testCallException() throws InterruptedException {
        Future<String> dummy = executor.submit(() -> {
            throw new IllegalStateException("DOH!");
        });

        try {
            dummy.get();
            fail("Exception expected");
        } catch (ExecutionException expected) {
            assertThat(expected.getCause().getMessage(), is(equalTo("DOH!")));
        }
    }

    @Test
    public void testBothCallAndCloseException() throws InterruptedException {
        throwingContextManager.initializeNewContext("The quick brown fox jumps over the lazy dog");
        ThrowingContextManager.onClose = new IllegalStateException("Sometimes we stare so long at a door that is closing " +
                "that we see too late the one that is open. --Alexander Graham Bell");
        Future<String> dummy = executor.submit(new Callable<String>() {
            public String call() {
                throw new IllegalStateException("DOH!");
            }
        });

        try {
            dummy.get();
            fail("Exception expected");
        } catch (ExecutionException expected) {
            assertThat(expected.getCause().getMessage(), is(equalTo("DOH!")));
        }
    }
}
