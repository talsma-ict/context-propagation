/*
 * Copyright 2016-2020 Talsma ICT
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
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.DummyContextManager;
import nl.talsmasoftware.context.ThrowingContextManager;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static nl.talsmasoftware.context.observer.Observed.Action.ACTIVATE;
import static nl.talsmasoftware.context.observer.Observed.Action.DEACTIVATE;
import static nl.talsmasoftware.context.observer.Observed.activated;
import static nl.talsmasoftware.context.observer.Observed.deactivated;
import static nl.talsmasoftware.context.observer.SimpleContextObserver.observed;
import static nl.talsmasoftware.context.observer.SimpleContextObserver.observedContextManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("deprecation")
public class ContextObserversTest {

    @BeforeEach
    @AfterEach
    public void clearObserved() {
        observedContextManager = null;
        observed.clear();
    }

    @Test
    public void testUnsupportedConstructor() throws IllegalAccessException, InstantiationException {
        Constructor<?>[] constructors = ContextObservers.class.getDeclaredConstructors();
        assertThat("Number of constructors", constructors.length, is(1));
        assertThat("Constructor parameters", constructors[0].getParameterTypes().length, is(0));
        assertThat("Constructor accessibility", constructors[0].isAccessible(), is(false));
        try {
            constructors[0].setAccessible(true);
            constructors[0].newInstance();
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

    @Test
    public void testUnobservableDeprecatedLegacyAbstractThreadLocalContext() {
        observedContextManager = DeprecatedContextManager.class;
        final ContextManager<String> manager = new DeprecatedContextManager();
        final Context<String> ctx = manager.initializeNewContext("Activated context");
        Context<String> ctx2 = null;
        try {
            assertThat(manager.getActiveContext().getValue(), is("Activated context"));
            assertThat(observed, not(hasItem(activated(is("Activated context")))));
            ctx2 = manager.initializeNewContext("Nested active context");

        } finally {
            if (ctx2 != null) ctx2.close();
            ctx.close();
        }
        assertThat(observed, not(hasItem(deactivated(is("Activated context")))));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOnActivate() {
        observedContextManager = DummyContextManager.class;
        Class reportedClass = ContextManager.class;
        ContextManagers.onActivate(reportedClass, "activated value", "previous value");
        assertThat(observed, is(empty()));

        observedContextManager = ContextManager.class;
        reportedClass = DummyContextManager.class;
        ContextObservers.onActivate(reportedClass, "activated value", "previous value");
        assertThat(observed, hasItem(activated(equalTo("activated value"))));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOnDeactivate() {
        observedContextManager = DummyContextManager.class;
        Class reportedClass = ContextManager.class;
        ContextObservers.onDeactivate(reportedClass, "deactivated value", "restored value");
        assertThat(observed, is(empty()));

        observedContextManager = ContextManager.class;
        reportedClass = DummyContextManager.class;
        ContextObservers.onDeactivate(reportedClass, "deactivated value", "restored value");
        assertThat(observed, hasItem(deactivated(equalTo("deactivated value"))));
    }
}
