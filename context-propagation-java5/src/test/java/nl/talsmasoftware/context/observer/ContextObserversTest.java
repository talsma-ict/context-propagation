/*
 * Copyright 2016-2019 Talsma ICT
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
package nl.talsmasoftware.context.observer;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManager;
import nl.talsmasoftware.context.DummyContextManager;
import nl.talsmasoftware.context.ThrowingContextManager;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static nl.talsmasoftware.context.observer.Observed.Action.ACTIVATE;
import static nl.talsmasoftware.context.observer.Observed.Action.DEACTIVATE;
import static nl.talsmasoftware.context.observer.SimpleContextObserver.observed;
import static nl.talsmasoftware.context.observer.SimpleContextObserver.observedContextManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class ContextObserversTest {

    @Before
    @After
    public void clearObserved() {
        observedContextManager = null;
        observed.clear();
    }

    @Test
    public void testUnsupportedConstructor() {
        Constructor<?>[] constructors = ContextObservers.class.getDeclaredConstructors();
        assertThat("Number of constructors", constructors.length, is(1));
        assertThat("Constructor parameters", constructors[0].getParameterTypes().length, is(0));
        assertThat("Constructor accessibility", constructors[0].isAccessible(), is(false));
        try {
            constructors[0].setAccessible(true);
            constructors[0].newInstance();
            fail("InvocationTargetException expected.");
        } catch (IllegalAccessException e) {
            fail("InvocationTargetException expected.");
        } catch (InstantiationException e) {
            fail("InvocationTargetException expected.");
        } catch (InvocationTargetException expected) {
            assertThat(expected.getCause(), is(instanceOf(UnsupportedOperationException.class)));
        }
    }

    @Test
    public void testSimpleContextObserver_observingNull() {
        final ContextManager<String> manager = new DummyContextManager();
        final Context<String> ctx = manager.initializeNewContext("Activated context");
        try {
            assertThat(manager.getActiveContext().getValue(), is("Activated context"));
            assertThat(observed, is(Matchers.<Observed>empty()));
        } finally {
            ctx.close();
        }
        assertThat(observed, is(Matchers.<Observed>empty()));
    }

    @Test
    public void testSimpleContextObserver_observingDifferentContextManager() {
        observedContextManager = ThrowingContextManager.class;
        final ContextManager<String> manager = new DummyContextManager();
        final Context<String> ctx = manager.initializeNewContext("Activated context");
        try {
            assertThat(manager.getActiveContext().getValue(), is("Activated context"));
            assertThat(observed, is(Matchers.<Observed>empty()));
        } finally {
            ctx.close();
        }
        assertThat(observed, is(Matchers.<Observed>empty()));
    }

    @Test
    public void testSimpleContextObserver_observingSpecificContextManager() {
        observedContextManager = DummyContextManager.class;
        final ContextManager<String> manager = new DummyContextManager();
        final Context<String> ctx = manager.initializeNewContext("Activated context");
        try {
            assertThat(manager.getActiveContext().getValue(), is("Activated context"));
            assertThat(observed, contains(new Observed(ACTIVATE, "Activated context", null)));
        } finally {
            ctx.close();
        }
        assertThat(observed, contains(
                new Observed(ACTIVATE, "Activated context", null),
                new Observed(DEACTIVATE, "Activated context", null)));
    }

    @Test
    public void testSimpleContextObserver_observingAnyContextManager() {
        observedContextManager = ContextManager.class;
        final ContextManager<String> manager = new DummyContextManager();
        final Context<String> ctx = manager.initializeNewContext("Activated context");
        try {
            assertThat(manager.getActiveContext().getValue(), is("Activated context"));
            assertThat(observed, contains(new Observed(ACTIVATE, "Activated context", null)));
        } finally {
            ctx.close();
        }
        assertThat(observed, contains(
                new Observed(ACTIVATE, "Activated context", null),
                new Observed(DEACTIVATE, "Activated context", null)));
    }

}
