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
package nl.talsmasoftware.context.clearable;

import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.core.ContextManagers;
import nl.talsmasoftware.context.dummy.ThrowingContextManager;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for clearing context managers.
 *
 * @author Sjoerd Talsma
 */
public class ClearableContextManagerTest {
    private static ContextManager<String> CLEARABLE = new ClearableDummyContextManager();
    private static ContextManager<String> AUTO_INITIALIZING = new AutoInitializingContextManager();

    @Test
    public void testClearActiveContexts_byManager() {
        assertThat(CLEARABLE, is(instanceOf(ContextManager.class)));

        CLEARABLE.initializeNewContext("First value");
        CLEARABLE.initializeNewContext("Second value");
        CLEARABLE.initializeNewContext("Third value");
        assertThat(CLEARABLE.getActiveContextValue(), is("Third value"));

        ContextManagers.clearActiveContexts();
        assertThat(CLEARABLE.getActiveContextValue(), is(nullValue()));
    }

    @Test
    public void testClearActiveContexts_exception() {
        ThrowingContextManager.onGet = new IllegalStateException("Cannot get the current active context!");

        ContextManagers.clearActiveContexts();
        // Mustn't throw exceptions.
    }
}
