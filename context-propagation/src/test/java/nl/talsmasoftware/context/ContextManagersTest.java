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
package nl.talsmasoftware.context;

import nl.talsmasoftware.context.executors.ContextAwareExecutorService;
import nl.talsmasoftware.context.observer.SimpleContextObserver;
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

import static nl.talsmasoftware.context.observer.Observed.activated;
import static nl.talsmasoftware.context.observer.Observed.deactivated;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Sjoerd Talsma
 */
public class ContextManagersTest {

    @BeforeEach
    @AfterEach
    public void resetContexts() {
        DummyContext.reset();
    }

    @BeforeEach
    @AfterEach
    public void resetContextClassLoader() {
        ContextManagers.useClassLoader(null);
    }

    @BeforeEach
    @AfterEach
    public void clearObserved() {
        SimpleContextObserver.observedContextManager = null;
        SimpleContextObserver.observed.clear();
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
        } catch (IllegalAccessException e) {
            fail("InvocationTargetException expected.");
        } catch (InstantiationException e) {
            fail("InvocationTargetException expected.");
        } catch (InvocationTargetException e) {
            assertThat(e.getCause(), is(instanceOf(UnsupportedOperationException.class)));
        }
    }

    @Test
    public void testSnapshot_inSameThread() {
        DummyContext.reset();
        assertThat(DummyContext.currentValue(), is(nullValue()));

        DummyContext ctx1 = new DummyContext("initial value");
        assertThat(DummyContext.currentValue(), is("initial value"));

        DummyContext ctx2 = new DummyContext("second value");
        assertThat(DummyContext.currentValue(), is("second value"));

        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        assertThat(DummyContext.currentValue(), is("second value")); // No context change because of snapshot.

        DummyContext ctx3 = new DummyContext("third value");
        assertThat(DummyContext.currentValue(), is("third value"));

        // Reactivate snapshot: ctx1 -> ctx2 -> ctx3 -> ctx2'
        Context<Void> ctxSnapshot = snapshot.reactivate();
        assertThat(DummyContext.currentValue(), is("second value"));

        ctxSnapshot.close();
        assertThat(DummyContext.currentValue(), is("third value")); // back to ctx3, NOT ctx1 !!

        // out-of-order closing!
        ctx2.close();
        assertThat(DummyContext.currentValue(), is("third value")); // back to ctx3, NOT ctx1 !!

        ctx3.close();
        assertThat(DummyContext.currentValue(), is("initial value")); // back to ctx1 because ctx2 is closed

        assertThat(ctx1.isClosed(), is(false));
        assertThat(ctx2.isClosed(), is(true));
        assertThat(ctx3.isClosed(), is(true));
        ctx1.close();
    }

    @Test
    public void testSnapshotThreadPropagation() throws ExecutionException, InterruptedException {
        DummyContext.reset();
        ExecutorService threadpool = new ContextAwareExecutorService(Executors.newCachedThreadPool());
        assertThat(DummyContext.currentValue(), is(nullValue()));

        DummyContext ctx1 = new DummyContext("initial value");
        assertThat(DummyContext.currentValue(), is("initial value"));
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
                DummyContext inThread = new DummyContext("in-thread value");
                try {
                    res += ", " + DummyContext.currentValue();
                } finally {
                    inThread.close();
                }
                return res + ", " + DummyContext.currentValue();
            }
        });
        assertThat(DummyContext.currentValue(), is("second value"));
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

        assertThat(DummyContext.currentValue(), is(nullValue()));
        Context<Void> reactivation = snapshot.reactivate();
        assertThat(DummyContext.currentValue(), is("blah"));
        reactivation.close();
        assertThat(DummyContext.current(), is(nullValue()));
    }

    @Test
    public void testReactivateSnapshot_ExceptionHandling() {
        final RuntimeException reactivationException = new IllegalStateException("Cannot create new context!");
        ThrowingContextManager mgr = new ThrowingContextManager();
        Context<String> ctx1 = new DummyContext("foo");
        Context<String> ctx2 = mgr.initializeNewContext("bar");
        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        ThrowingContextManager.onInitialize = reactivationException;

        assertThat(DummyContext.currentValue(), is("foo"));
        assertThat(mgr.getActiveContext().getValue(), is("bar"));
        ctx1.close();
        ctx2.close();

        assertThat(DummyContext.currentValue(), is(nullValue()));
        assertThat(mgr.getActiveContext(), is(nullValue()));
        try {
            snapshot.reactivate();
            fail("Exception expected");
        } catch (RuntimeException expected) {
            assertThat(expected, is(sameInstance(reactivationException)));
        }
        // foo + bar mustn't be set after exception!
        assertThat(DummyContext.currentValue(), is(nullValue()));
        assertThat(mgr.getActiveContext(), is(nullValue()));
    }

    @Test
    public void testConcurrentSnapshots_fixedClassLoader() throws ExecutionException, InterruptedException {
        ContextManagers.useClassLoader(Thread.currentThread().getContextClassLoader());
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
    @SuppressWarnings("unchecked")
    public void testOnActivate() {
        SimpleContextObserver.observedContextManager = DummyContextManager.class;
        Class reportedClass = ContextManager.class;
        ContextManagers.onActivate(reportedClass, "activated value", "previous value");
        assertThat(SimpleContextObserver.observed, is(empty()));

        SimpleContextObserver.observedContextManager = ContextManager.class;
        reportedClass = DummyContextManager.class;
        ContextManagers.onActivate(reportedClass, "activated value", "previous value");
        assertThat(SimpleContextObserver.observed, hasItem(activated(equalTo("activated value"))));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOnDeactivate() {
        SimpleContextObserver.observedContextManager = DummyContextManager.class;
        Class reportedClass = ContextManager.class;
        ContextManagers.onDeactivate(reportedClass, "deactivated value", "restored value");
        assertThat(SimpleContextObserver.observed, is(empty()));

        SimpleContextObserver.observedContextManager = ContextManager.class;
        reportedClass = DummyContextManager.class;
        ContextManagers.onDeactivate(reportedClass, "deactivated value", "restored value");
        assertThat(SimpleContextObserver.observed, hasItem(deactivated(equalTo("deactivated value"))));
    }

}
