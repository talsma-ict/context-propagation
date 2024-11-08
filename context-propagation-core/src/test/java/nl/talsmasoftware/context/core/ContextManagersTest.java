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
package nl.talsmasoftware.context.core;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.core.concurrent.ContextAwareExecutorService;
import nl.talsmasoftware.context.dummy.DummyContext;
import nl.talsmasoftware.context.dummy.DummyContextManager;
import nl.talsmasoftware.context.dummy.ThrowingContextManager;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Sjoerd Talsma
 */
public class ContextManagersTest {
    DummyContextManager dummyManager = new DummyContextManager();

    @BeforeEach
    @AfterEach
    public void resetContexts() {
        ContextManagers.clearActiveContexts();
    }

    @BeforeEach
    @AfterEach
    public void resetContextClassLoader() {
        ContextManagers.useClassLoader(null);
    }

    @Test
    public void testUnsupportedConstructor() {
        Constructor<?>[] constructors = ContextManagers.class.getDeclaredConstructors();
        assertThat("Number of constructors", constructors.length, is(1));
        assertThat("Constructor parameters", constructors[0].getParameterTypes().length, is(0));
        assertThat("Constructor accessibility", constructors[0].isAccessible(), is(false));
        try {
            constructors[0].setAccessible(true);
            constructors[0].newInstance();
            fail("Exception expected.");
        } catch (IllegalAccessException | InstantiationException e) {
            fail("InvocationTargetException expected.");
        } catch (InvocationTargetException e) {
            assertThat(e.getCause(), is(instanceOf(UnsupportedOperationException.class)));
        }
    }

    @Test
    public void testSnapshot_inSameThread() {
        dummyManager.clear();
        MatcherAssert.assertThat(DummyContext.currentValue(), is(nullValue()));

        DummyContext ctx1 = new DummyContext("initial value");
        MatcherAssert.assertThat(DummyContext.currentValue(), is("initial value"));

        DummyContext ctx2 = new DummyContext("second value");
        MatcherAssert.assertThat(DummyContext.currentValue(), is("second value"));

        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        MatcherAssert.assertThat(DummyContext.currentValue(), is("second value")); // No context change because of snapshot.

        DummyContext ctx3 = new DummyContext("third value");
        MatcherAssert.assertThat(DummyContext.currentValue(), is("third value"));

        // Reactivate snapshot: ctx1 -> ctx2 -> ctx3 -> ctx2'
        ContextSnapshot.Reactivation reactivation = snapshot.reactivate();
        MatcherAssert.assertThat(DummyContext.currentValue(), is("second value"));

        reactivation.close();
        MatcherAssert.assertThat(DummyContext.currentValue(), is("third value")); // back to ctx3, NOT ctx1 !!

        // out-of-order closing!
        ctx2.close();
        MatcherAssert.assertThat(DummyContext.currentValue(), is("third value")); // back to ctx3, NOT ctx1 !!

        ctx3.close();
        MatcherAssert.assertThat(DummyContext.currentValue(), is("initial value")); // back to ctx1 because ctx2 is closed

        MatcherAssert.assertThat(ctx1.isClosed(), is(false));
        MatcherAssert.assertThat(ctx2.isClosed(), is(true));
        MatcherAssert.assertThat(ctx3.isClosed(), is(true));
        ctx1.close();
    }

    @Test
    public void testSnapshotThreadPropagation() throws ExecutionException, InterruptedException {
        DummyContext.reset();
        ExecutorService threadpool = new ContextAwareExecutorService(Executors.newCachedThreadPool());
        MatcherAssert.assertThat(DummyContext.currentValue(), is(nullValue()));

        DummyContext ctx1 = new DummyContext("initial value");
        MatcherAssert.assertThat(DummyContext.currentValue(), is("initial value"));
        Future<String> threadResult = threadpool.submit(new Callable<String>() {
            public String call() throws Exception {
                return DummyContext.currentValue();
            }
        });
        assertThat(threadResult.get(), is("initial value"));

        DummyContext ctx2 = new DummyContext("second value");
        threadResult = threadpool.submit(new Callable<String>() {
            public String call() throws Exception {
                String res = DummyContext.currentValue();
                try (DummyContext inThread = new DummyContext("in-thread value")) {
                    res += ", " + DummyContext.currentValue();
                }
                return res + ", " + DummyContext.currentValue();
            }
        });
        MatcherAssert.assertThat(DummyContext.currentValue(), is("second value"));
        assertThat(threadResult.get(), is("second value, in-thread value, second value"));

        ctx2.close();
        ctx1.close();
    }

    @Test
    public void testConcurrentSnapshots() throws ExecutionException, InterruptedException {
        int threadcount = 25;
        ExecutorService threadpool = Executors.newFixedThreadPool(threadcount);
        try {
            List<Future<ContextSnapshot>> snapshots = new ArrayList<Future<ContextSnapshot>>(threadcount);
            for (int i = 0; i < threadcount; i++) {
                snapshots.add(threadpool.submit(new Callable<ContextSnapshot>() {
                    public ContextSnapshot call() throws Exception {
                        return ContextManagers.createContextSnapshot();
                    }
                }));
            }

            for (int i = 0; i < threadcount; i++) {
                assertThat(snapshots.get(i).get(), is(notNullValue()));
            }
        } finally {
            threadpool.shutdown();
        }
    }

    @Test
    public void testCreateSnapshot_ExceptionHandling() {
        ThrowingContextManager.onGet = new IllegalStateException("No active context!");
        Context<String> ctx = new DummyContext("blah");
        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        ctx.close();

        MatcherAssert.assertThat(DummyContext.currentValue(), is(nullValue()));
        ContextSnapshot.Reactivation reactivation = snapshot.reactivate();
        MatcherAssert.assertThat(DummyContext.currentValue(), is("blah"));
        reactivation.close();
        MatcherAssert.assertThat(DummyContext.currentValue(), is(nullValue()));
    }

    @Test
    public void testReactivateSnapshot_ExceptionHandling() {
        final RuntimeException reactivationException = new IllegalStateException("Cannot create new context!");
        ThrowingContextManager mgr = new ThrowingContextManager();
        Context<String> ctx1 = new DummyContext("foo");
        Context<String> ctx2 = mgr.initializeNewContext("bar");
        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        ThrowingContextManager.onInitialize = reactivationException;

        MatcherAssert.assertThat(DummyContext.currentValue(), is("foo"));
        MatcherAssert.assertThat(mgr.getActiveContextValue(), is("bar"));
        ctx1.close();
        ctx2.close();

        MatcherAssert.assertThat(DummyContext.currentValue(), is(nullValue()));
        MatcherAssert.assertThat(mgr.getActiveContextValue(), is(nullValue()));
        RuntimeException expected = assertThrows(RuntimeException.class, snapshot::reactivate);

        // foo + bar mustn't be set after exception!
        MatcherAssert.assertThat(DummyContext.currentValue(), is(nullValue()));
        MatcherAssert.assertThat(mgr.getActiveContextValue(), is(nullValue()));
    }

    @Test
    public void testConcurrentSnapshots_fixedClassLoader() throws ExecutionException, InterruptedException {
        ContextManagers.useClassLoader(Thread.currentThread().getContextClassLoader());
        int threadcount = 25;
        ExecutorService threadpool = Executors.newFixedThreadPool(threadcount);
        try {
            Future<ContextSnapshot>[] snapshots = new Future[threadcount];
            for (int i = 0; i < threadcount; i++) {
                snapshots[i] = threadpool.submit(ContextManagers::createContextSnapshot);
            }

            for (int i = 0; i < threadcount; i++) {
                assertThat("Future " + i, snapshots[i], notNullValue());
                assertThat("Snapshot " + i, snapshots[i].get(), notNullValue());
            }
        } finally {
            threadpool.shutdown();
        }
    }

}
