/*
 * Copyright 2016-2021 Talsma ICT
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
package nl.talsmasoftware.context.log4j2.threadcontext;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.executors.ContextAwareExecutorService;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasToString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for the {@link Log4j2ThreadContextManager}.
 */
class Log4j2ThreadContextManagerTest {

    /**
     * Underlying executor of {@link #threadpool}; not context-aware
     */
    ExecutorService rawThreadpool;
    /**
     * Context-aware executor
     */
    ExecutorService threadpool;

    @BeforeEach
    void setupThreadpool() {
        // Only use 1 thread because tests expect single thread to be reused
        rawThreadpool = Executors.newFixedThreadPool(1);
        threadpool = new ContextAwareExecutorService(rawThreadpool);
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
        return new Callable<String>() {
            public String call() {
                return ThreadContext.get(key);
            }
        };
    }

    private static Callable<String> createGetStackValue(final int index) {
        return new Callable<String>() {
            public String call() {
                return ThreadContext.getImmutableStack().asList().get(index);
            }
        };
    }

    @Test
    void testProvider() {
        assertSame(Log4j2ThreadContextManager.INSTANCE, Log4j2ThreadContextManager.provider());

        // Verify that no-arg constructor, used by ServiceLoader, works
        assertDoesNotThrow(new Executable() {
            @SuppressWarnings("deprecation")
            public void execute() {
                new Log4j2ThreadContextManager();
            }
        });
    }

    @Test
    void testInitializeNewContext_null() {
        assertThrows(NullPointerException.class, new Executable() {
            public void execute() {
                Log4j2ThreadContextManager.INSTANCE.initializeNewContext(null);
            }
        });
    }

    /**
     * Verify that calling {@code close()} on the result of {@link Log4j2ThreadContextManager#getActiveContext()}
     * has no effect because it is managed by Log4j 2.
     */
    @Test
    void testGetActiveContext_close() {
        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        // Should have no effect
        Log4j2ThreadContextManager.INSTANCE.getActiveContext().close();

        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("map1", "value1");
        expectedMap.put("map2", "value2");
        assertEquals(expectedMap, ThreadContext.getContext());

        List<String> expectedStack = Arrays.asList("stack1", "stack2");
        assertEquals(expectedStack, ThreadContext.getImmutableStack().asList());
    }

    @Test
    void test_ThreadContextPropagation() throws ExecutionException, InterruptedException {
        String mapKey = "map1";
        ThreadContext.put(mapKey, "value1");
        ThreadContext.push("stack1");
        Future<String> mapValue = threadpool.submit(createGetMapValue(mapKey));
        assertEquals("value1", mapValue.get());
        Future<String> stackValue = threadpool.submit(createGetStackValue(0));
        assertEquals("stack1", stackValue.get());
    }

    /**
     * Verify that existing {@code ThreadContext} data is only overwritten
     * in case of conflict.
     */
    @Test
    void test_ThreadContextPropagation_existing() throws ExecutionException, InterruptedException {
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
        assertEquals("value1", mapValue1.get()); // Was overwritten
        Future<String> mapValue2 = threadpool.submit(createGetMapValue("map2"));
        assertEquals("old-value2", mapValue2.get());
        Future<String> mapValue3 = threadpool.submit(createGetMapValue("map3"));
        assertEquals("value3", mapValue3.get());

        Future<String> stackValue1 = threadpool.submit(createGetStackValue(0));
        assertEquals("stack1", stackValue1.get());
        Future<String> stackValue2 = threadpool.submit(createGetStackValue(1));
        assertEquals("stack2", stackValue2.get());

        // After clearing context in current thread, worker thread should
        // still have its previous context; empty thread context should
        // not overwrite existing one on propagation
        ThreadContext.clearAll();

        mapValue1 = threadpool.submit(createGetMapValue("map1"));
        assertEquals("old-value1", mapValue1.get());
        mapValue2 = threadpool.submit(createGetMapValue("map2"));
        assertEquals("old-value2", mapValue2.get());
        Future<Integer> mapSize = threadpool.submit(new Callable<Integer>() {
            public Integer call() {
                return ThreadContext.getContext().size();
            }
        });
        assertEquals(2, mapSize.get());

        stackValue1 = threadpool.submit(createGetStackValue(0));
        assertEquals("stack1", stackValue1.get());
        Future<Integer> stackSize = threadpool.submit(new Callable<Integer>() {
            public Integer call() {
                return ThreadContext.getDepth();
            }
        });
        assertEquals(1, stackSize.get());
    }

    @Test
    void test_SnapshotRestoration() {
        String mapKey1 = "map1";
        ThreadContext.put(mapKey1, "value1");
        ThreadContext.push("stack1");

        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        assertEquals("value1", ThreadContext.get(mapKey1), "New snapshot shouldn't manipulate ThreadContext map");
        assertEquals(1, ThreadContext.getContext().size());
        assertEquals("stack1", ThreadContext.peek(), "New snapshot shouldn't manipulate ThreadContext stack");
        assertEquals(1, ThreadContext.getDepth());

        ThreadContext.put(mapKey1, "value1-new");
        assertEquals("value1-new", ThreadContext.get(mapKey1), "Sanity check: ThreadContext map changed");
        ThreadContext.push("stack2");
        assertEquals("stack2", ThreadContext.peek(), "Sanity check: ThreadContext stack changed");

        String mapKey2 = "map2";
        ThreadContext.put(mapKey2, "value2");

        Context<Void> reactivation = snapshot.reactivate();
        assertEquals("value1", ThreadContext.get(mapKey1), "ThreadContext changed by reactivation");
        assertEquals("value2", ThreadContext.get(mapKey2), "Existing ThreadContext data should not have been cleared");
        assertEquals(2, ThreadContext.getContext().size());

        List<String> expectedStack = Arrays.asList("stack1", "stack2", "stack1");
        assertEquals(expectedStack, ThreadContext.getImmutableStack().asList(), "Stack value should have been pushed on existing stack");

        reactivation.close();
        assertEquals("value1-new", ThreadContext.get(mapKey1), "ThreadContext restored");
        assertEquals("value2", ThreadContext.get(mapKey2), "Existing ThreadContext data should not have been cleared");
        assertEquals(2, ThreadContext.getContext().size());

        expectedStack = Arrays.asList("stack1", "stack2");
        assertEquals(expectedStack, ThreadContext.getImmutableStack().asList(), "Last stack value should have been removed again");
    }

    @Test
    void testToString() {
        assertThat(Log4j2ThreadContextManager.INSTANCE.toString(), hasToString("Log4j2ThreadContextManager"));
    }

    @Test
    void testContextToString() {
        ThreadContext.put("map1", "value1");
        ThreadContext.push("stack1");

        Log4j2ThreadContextSnapshot data = Log4j2ThreadContextSnapshot.captureFromCurrentThread();

        Log4j2ThreadContextManager mgr = Log4j2ThreadContextManager.INSTANCE;

        assertThat(mgr.getActiveContext(), hasToString("ThreadContextContext{closed}"));
        Context<Log4j2ThreadContextSnapshot> ctx = mgr.initializeNewContext(data);
        try {
            assertThat(ctx, hasToString("ThreadContextContext{" + data + "}"));
        } finally {
            ctx.close();
        }
    }

    @Test
    void testClearActiveContexts() {
        ThreadContext.put("map1", "value1");
        ThreadContext.push("stack1");
        ContextManagers.clearActiveContexts();
        assertTrue(ThreadContext.isEmpty());
        assertEquals(0, ThreadContext.getDepth());
    }
}
