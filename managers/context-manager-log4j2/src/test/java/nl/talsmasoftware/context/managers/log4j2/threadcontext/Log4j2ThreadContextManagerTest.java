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
package nl.talsmasoftware.context.managers.log4j2.threadcontext;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.core.concurrent.ContextAwareExecutorService;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test for the {@link Log4j2ThreadContextManager}.
 */
class Log4j2ThreadContextManagerTest {

    /**
     * Underlying executor of {@link #threadpool}; not context-aware
     */
    private ExecutorService rawThreadpool;

    /**
     * Context-aware executor
     */
    private ExecutorService threadpool;

    @BeforeEach
    void setupThreadpool() {
        // Only use 1 thread because tests expect single thread to be reused
        rawThreadpool = Executors.newFixedThreadPool(1);
        threadpool = ContextAwareExecutorService.wrap(rawThreadpool);
    }

    @AfterEach
    void shutdownThreadpool() {
        threadpool.shutdown();
    }

    @BeforeEach
    @AfterEach
    void clearThreadContext() {
        ThreadContext.clearAll();
    }

    private static Callable<String> createGetMapValue(final String key) {
        return () -> ThreadContext.get(key);
    }

    private static Callable<String> createGetStackValue(final int index) {
        return () -> ThreadContext.getImmutableStack().asList().get(index);
    }

    @Test
    void testProvider() {
        Log4j2ThreadContextManager first = Log4j2ThreadContextManager.provider();
        Log4j2ThreadContextManager second = Log4j2ThreadContextManager.provider();
        assertThat(second).isSameAs(first);
    }

    @Test
    @SuppressWarnings("deprecation")
    void testDefaultConstructor() {
        assertThat(new Log4j2ThreadContextManager()).isNotSameAs(Log4j2ThreadContextManager.provider());
    }

    @Test
    void testActivate_null() {
        Log4j2ThreadContextManager manager = Log4j2ThreadContextManager.provider();
        assertThatThrownBy(() -> manager.activate(null))
                .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);
    }

    @Test
    void testPropagationOfThreadContextValues() throws ExecutionException, InterruptedException {
        String mapKey = "map1";
        ThreadContext.put(mapKey, "value1");
        ThreadContext.push("stack1");
        Future<String> mapValue = threadpool.submit(createGetMapValue(mapKey));
        assertThat(mapValue.get()).isEqualTo("value1");
        Future<String> stackValue = threadpool.submit(createGetStackValue(0));
        assertThat(stackValue.get()).isEqualTo("stack1");
    }

    /**
     * Verify that existing {@code ThreadContext} data is only overwritten
     * in case of conflict.
     */
    @Test
    void testPropagationOfThreadContextIntoThreadWithPreExistingValues() throws ExecutionException, InterruptedException {
        // Pretend there exists already data for the thread which was for example
        // initialized when the thread was constructed
        rawThreadpool.submit(new Runnable() {
            public void run() {
                ThreadContext.put("map1", "old-value1");
                ThreadContext.put("map2", "old-value2");
                ThreadContext.push("stack1");
            }
        }).get();

        ThreadContext.put("map1", "value1");
        ThreadContext.put("map3", "value3");
        ThreadContext.push("stack2");
        Future<String> mapValue1 = threadpool.submit(createGetMapValue("map1"));
        assertThat(mapValue1.get()).isEqualTo("value1"); // Was overwritten
        Future<String> mapValue2 = threadpool.submit(createGetMapValue("map2"));
        assertThat(mapValue2.get()).isEqualTo("old-value2");
        Future<String> mapValue3 = threadpool.submit(createGetMapValue("map3"));
        assertThat(mapValue3.get()).isEqualTo("value3");

        Future<String> stackValue1 = threadpool.submit(createGetStackValue(0));
        assertThat(stackValue1.get()).isEqualTo("stack1");
        Future<String> stackValue2 = threadpool.submit(createGetStackValue(1));
        assertThat(stackValue2.get()).isEqualTo("stack2");

        // After clearing context in current thread, worker thread should
        // still have its previous context; empty thread context should
        // not overwrite existing one on propagation
        ThreadContext.clearAll();

        mapValue1 = threadpool.submit(createGetMapValue("map1"));
        assertThat(mapValue1.get()).isEqualTo("old-value1");
        mapValue2 = threadpool.submit(createGetMapValue("map2"));
        assertThat(mapValue2.get()).isEqualTo("old-value2");
        Future<Integer> mapSize = threadpool.submit(new Callable<Integer>() {
            public Integer call() {
                return ThreadContext.getContext().size();
            }
        });
        assertThat(mapSize.get()).isEqualTo(2);

        stackValue1 = threadpool.submit(createGetStackValue(0));
        assertThat(stackValue1.get()).isEqualTo("stack1");
        Future<Integer> stackSize = threadpool.submit(new Callable<Integer>() {
            public Integer call() {
                return ThreadContext.getDepth();
            }
        });
        assertThat(stackSize.get()).isEqualTo(1);
    }

    @Test
    void testSnapshotRestorationAfterClosingReactivatedSnapshot() {
        String mapKey1 = "map1";
        ThreadContext.put(mapKey1, "value1");
        ThreadContext.push("stack1");

        ContextSnapshot snapshot = ContextSnapshot.capture();
        assertThat(ThreadContext.get(mapKey1)).withFailMessage("New snapshot shouldn't manipulate ThreadContext map").isEqualTo("value1");
        assertThat(ThreadContext.getContext()).hasSize(1);
        assertThat(ThreadContext.peek()).withFailMessage("New snapshot shouldn't manipulate ThreadContext stack").isEqualTo("stack1");
        assertThat(ThreadContext.getDepth()).isEqualTo(1);

        ThreadContext.put(mapKey1, "value1-new");
        assertThat(ThreadContext.get(mapKey1)).withFailMessage("Sanity check: ThreadContext map changed").isEqualTo("value1-new");
        ThreadContext.push("stack2");
        assertThat(ThreadContext.peek()).withFailMessage("Sanity check: ThreadContext stack changed").isEqualTo("stack2");

        String mapKey2 = "map2";
        ThreadContext.put(mapKey2, "value2");

        ContextSnapshot.Reactivation reactivation = snapshot.reactivate();
        assertThat(ThreadContext.get(mapKey1)).withFailMessage("ThreadContext changed by reactivation").isEqualTo("value1");
        assertThat(ThreadContext.get(mapKey2)).withFailMessage("Existing ThreadContext data should not have been cleared").isEqualTo("value2");
        assertThat(ThreadContext.getContext()).hasSize(2);

        List<String> expectedStack = Arrays.asList("stack1", "stack2", "stack1");
        assertThat(ThreadContext.getImmutableStack().asList()).withFailMessage("Stack value should have been pushed on existing stack").isEqualTo(expectedStack);

        reactivation.close();
        assertThat(ThreadContext.get(mapKey1)).as("ThreadContext restored").isEqualTo("value1-new");
        assertThat(ThreadContext.get(mapKey2)).withFailMessage("Existing ThreadContext data should not have been cleared").isEqualTo("value2");
        assertThat(ThreadContext.getContext()).hasSize(2);

        expectedStack = Arrays.asList("stack1", "stack2");
        assertThat(ThreadContext.getImmutableStack().asList()).withFailMessage("Last stack value should have been removed again").isEqualTo(expectedStack);
    }

    @Test
    void testToString() {
        assertThat(Log4j2ThreadContextManager.provider()).hasToString("Log4j2ThreadContextManager");
    }

    @Test
    void testContextToString() {
        ThreadContext.put("key1", "value1");
        ThreadContext.push("stack1");

        Log4j2ThreadContextSnapshot data = Log4j2ThreadContextSnapshot.captureFromCurrentThread();

        final Log4j2ThreadContextManager mgr = Log4j2ThreadContextManager.provider();

        Context ctx = mgr.activate(data);
        try {
            assertThat(ctx).hasToString("ManagedLog4j2ThreadContext{" + data + "}");
        } finally {
            ctx.close();
            assertThat(ctx).hasToString("ManagedLog4j2ThreadContext{closed}");
        }
    }

    @Test
    void testClearActiveContexts() {
        ThreadContext.put("map1", "value1");
        ThreadContext.push("stack1");

        ContextManager.clearAll();

        assertThat(ThreadContext.isEmpty()).isTrue();
        assertThat(ThreadContext.getDepth()).isEqualTo(0);
    }
}
