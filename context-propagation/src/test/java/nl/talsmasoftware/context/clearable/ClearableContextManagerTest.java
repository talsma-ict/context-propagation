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
package nl.talsmasoftware.context.clearable;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import nl.talsmasoftware.context.ContextManager;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.DummyContextManager;
import nl.talsmasoftware.context.ThrowingContextManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for clearing context managers.
 *
 * @author Sjoerd Talsma
 */
public class ClearableContextManagerTest {
    private static ContextManager<String> CLEARABLE = new ClearableDummyContextManager();
    private static ContextManager<String> CONTEXT_CLEARABLE = new DummyManagerOfClearableContext();
    private static ContextManager<String> AUTO_INITIALIZING = new AutoInitializingContextManager();
    private static ContextManager<String> MANAGER_OF_ABSTRACTTLC = new DummyContextManager();

    @BeforeAll
    public static void initLogback() {
        if (!SLF4JBridgeHandler.isInstalled()) {
            /* Initialize SLF4J bridge. This re-routes logging through java.util.logging to SLF4J. */
            java.util.logging.LogManager.getLogManager().reset();
            SLF4JBridgeHandler.install();
            LoggerFactory.getILoggerFactory();
        }
        ((Logger) LoggerFactory.getLogger(ContextManagers.class)).setLevel(Level.ALL);
    }

    @Test
    public void testClearActiveContexts_byManager() {
        assertThat(CLEARABLE, is(instanceOf(Clearable.class)));

        CLEARABLE.initializeNewContext("First value");
        CLEARABLE.initializeNewContext("Second value");
        CLEARABLE.initializeNewContext("Third value");
        assertThat(CLEARABLE.getActiveContext().getValue(), is("Third value"));

        ContextManagers.clearActiveContexts();
        assertThat(CLEARABLE.getActiveContext(), is(nullValue()));
    }

    @Test
    public void testClearActiveContexts_byClearableContext() {
        assertThat(CONTEXT_CLEARABLE, is(not(instanceOf(Clearable.class))));

        CONTEXT_CLEARABLE.initializeNewContext("First value");
        CONTEXT_CLEARABLE.initializeNewContext("Second value");
        CONTEXT_CLEARABLE.initializeNewContext("Third value");
        assertThat(CONTEXT_CLEARABLE.getActiveContext(), is(instanceOf(Clearable.class)));
        assertThat(CONTEXT_CLEARABLE.getActiveContext().getValue(), is("Third value"));

        ContextManagers.clearActiveContexts();
        assertThat(CONTEXT_CLEARABLE.getActiveContext(), is(nullValue()));
    }

    @Test
    public void testClearActiveContexts_byAbstractTLC() {
        assertThat(MANAGER_OF_ABSTRACTTLC, is(not(instanceOf(Clearable.class))));

        MANAGER_OF_ABSTRACTTLC.initializeNewContext("First value");
        MANAGER_OF_ABSTRACTTLC.initializeNewContext("Second value");
        MANAGER_OF_ABSTRACTTLC.initializeNewContext("Third value");
        assertThat(MANAGER_OF_ABSTRACTTLC.getActiveContext().getValue(), is("Third value"));

        ContextManagers.clearActiveContexts();
        assertThat(MANAGER_OF_ABSTRACTTLC.getActiveContext(), is(nullValue()));
    }

    @Test
    public void testClearActiveContexts_autoReinitializeNewContext() {
        assertThat(AUTO_INITIALIZING, is(not(instanceOf(Clearable.class))));

        AUTO_INITIALIZING.initializeNewContext("First value");
        AUTO_INITIALIZING.initializeNewContext("Second value");
        AUTO_INITIALIZING.initializeNewContext("Third value");
        assertThat(AUTO_INITIALIZING.getActiveContext().getValue(), is("Third value"));

        ContextManagers.clearActiveContexts();
        assertThat(AUTO_INITIALIZING.getActiveContext().getValue(), is(nullValue()));
    }

    @Test
    public void testClearActiveContexts_exception() {
        ThrowingContextManager.onGet = new IllegalStateException("Cannot get the current active context!");

        ContextManagers.clearActiveContexts();
        // Mustn't throw exceptions.
    }
}
