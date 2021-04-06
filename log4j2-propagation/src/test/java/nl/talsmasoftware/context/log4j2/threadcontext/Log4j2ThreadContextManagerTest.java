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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        Log4j2ThreadContextManager first = Log4j2ThreadContextManager.provider();
        Log4j2ThreadContextManager second = Log4j2ThreadContextManager.provider();
        assertThat(second, sameInstance(first));
    }

    @Test
    @Deprecated
    void testDefaultConstructor() {
        assertThat(new Log4j2ThreadContextManager(), not(sameInstance(Log4j2ThreadContextManager.provider())));
    }

    @Test
    void testInitializeNewContext_null() {
        NullPointerException expected = assertThrows(NullPointerException.class, new Executable() {
            public void execute() {
                Log4j2ThreadContextManager.provider().initializeNewContext(null);
            }
        });
        assertThat(expected.getMessage(), notNullValue());
    }

    /**
     * Verify that calling {@code close()} on the result of {@link Log4j2ThreadContextManager#getActiveContext()}
     * has no effect because it is managed by Log4j2.
     */
    @Test
    void testGetActiveContext_close() {
        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        // Should have no effect
        Log4j2ThreadContextManager.provider().getActiveContext().close();

        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("map1", "value1");
        expectedMap.put("map2", "value2");
        assertThat(ThreadContext.getContext(), equalTo(expectedMap));

        List<String> expectedStack = Arrays.asList("stack1", "stack2");
        assertThat(ThreadContext.getImmutableStack().asList(), equalTo(expectedStack));
    }

    @Test
    void testThreadContextPropagation() throws ExecutionException, InterruptedException {
        String mapKey = "map1";
        ThreadContext.put(mapKey, "value1");
        ThreadContext.push("stack1");
        Future<String> mapValue = threadpool.submit(createGetMapValue(mapKey));
        assertThat(mapValue.get(), equalTo("value1"));
        Future<String> stackValue = threadpool.submit(createGetStackValue(0));
        assertThat(stackValue.get(), equalTo("stack1"));
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
        assertThat(mapValue1.get(), equalTo("value1")); // Was overwritten
        Future<String> mapValue2 = threadpool.submit(createGetMapValue("map2"));
        assertThat(mapValue2.get(), equalTo("old-value2"));
        Future<String> mapValue3 = threadpool.submit(createGetMapValue("map3"));
        assertThat(mapValue3.get(), equalTo("value3"));

        Future<String> stackValue1 = threadpool.submit(createGetStackValue(0));
        assertThat(stackValue1.get(), equalTo("stack1"));
        Future<String> stackValue2 = threadpool.submit(createGetStackValue(1));
        assertThat(stackValue2.get(), equalTo("stack2"));

        // After clearing context in current thread, worker thread should
        // still have its previous context; empty thread context should
        // not overwrite existing one on propagation
        ThreadContext.clearAll();

        mapValue1 = threadpool.submit(createGetMapValue("map1"));
        assertThat(mapValue1.get(), equalTo("old-value1"));
        mapValue2 = threadpool.submit(createGetMapValue("map2"));
        assertThat(mapValue2.get(), equalTo("old-value2"));
        Future<Integer> mapSize = threadpool.submit(new Callable<Integer>() {
            public Integer call() {
                return ThreadContext.getContext().size();
            }
        });
        assertThat(mapSize.get(), is(2));

        stackValue1 = threadpool.submit(createGetStackValue(0));
        assertThat(stackValue1.get(), equalTo("stack1"));
        Future<Integer> stackSize = threadpool.submit(new Callable<Integer>() {
            public Integer call() {
                return ThreadContext.getDepth();
            }
        });
        assertThat(stackSize.get(), is(1));
    }

    @Test
    void test_SnapshotRestoration() {
        String mapKey1 = "map1";
        ThreadContext.put(mapKey1, "value1");
        ThreadContext.push("stack1");

        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        assertThat("New snapshot shouldn't manipulate ThreadContext map", ThreadContext.get(mapKey1), equalTo("value1"));
        assertThat(ThreadContext.getContext().size(), is(1));
        assertThat("New snapshot shouldn't manipulate ThreadContext stack", ThreadContext.peek(), equalTo("stack1"));
        assertThat(ThreadContext.getDepth(), is(1));

        ThreadContext.put(mapKey1, "value1-new");
        assertThat("Sanity check: ThreadContext map changed", ThreadContext.get(mapKey1), equalTo("value1-new"));
        ThreadContext.push("stack2");
        assertThat("Sanity check: ThreadContext stack changed", ThreadContext.peek(), equalTo("stack2"));

        String mapKey2 = "map2";
        ThreadContext.put(mapKey2, "value2");

        Context<Void> reactivation = snapshot.reactivate();
        assertThat("ThreadContext changed by reactivation", ThreadContext.get(mapKey1), equalTo("value1"));
        assertThat("Existing ThreadContext data should not have been cleared", ThreadContext.get(mapKey2), equalTo("value2"));
        assertThat(ThreadContext.getContext().size(), is(2));

        List<String> expectedStack = Arrays.asList("stack1", "stack2", "stack1");
        assertThat("Stack value should have been pushed on existing stack", ThreadContext.getImmutableStack().asList(), equalTo(expectedStack));

        reactivation.close();
        assertThat("ThreadContext restored", ThreadContext.get(mapKey1), equalTo("value1-new"));
        assertThat("Existing ThreadContext data should not have been cleared", ThreadContext.get(mapKey2), equalTo("value2"));
        assertThat(ThreadContext.getContext().size(), is(2));

        expectedStack = Arrays.asList("stack1", "stack2");
        assertThat("Last stack value should have been removed again", ThreadContext.getImmutableStack().asList(), equalTo(expectedStack));
    }

    @Test
    void testToString() {
        assertThat(Log4j2ThreadContextManager.provider(), hasToString("Log4j2ThreadContextManager"));
    }

    @Test
    void testContextToString() {
        ThreadContext.put("key1", "value1");
        ThreadContext.push("stack1");

        Log4j2ThreadContextSnapshot data = Log4j2ThreadContextSnapshot.captureFromCurrentThread();

        final Log4j2ThreadContextManager mgr = Log4j2ThreadContextManager.provider();

        assertThat(mgr.getActiveContext(), hasToString("ReadonlyLog4j2ThreadContext{" + data + "}"));
        Context<Log4j2ThreadContextSnapshot> ctx = mgr.initializeNewContext(data);
        try {
            assertThat(ctx, hasToString("ManagedLog4j2ThreadContext{" + data + "}"));
        } finally {
            ctx.close();
            assertThat(ctx, hasToString("ManagedLog4j2ThreadContext{closed}"));
        }
    }

    @Test
    void testClearActiveContexts() {
        ThreadContext.put("map1", "value1");
        ThreadContext.push("stack1");

        ContextManagers.clearActiveContexts();

        assertThat(ThreadContext.isEmpty(), is(true));
        assertThat(ThreadContext.getDepth(), is(0));
    }
}
