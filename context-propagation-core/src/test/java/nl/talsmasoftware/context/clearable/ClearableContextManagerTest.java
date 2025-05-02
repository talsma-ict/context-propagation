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
package nl.talsmasoftware.context.clearable;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.dummy.ThrowingContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Tests for clearing context managers.
 *
 * @author Sjoerd Talsma
 */
class ClearableContextManagerTest {
    private static final ContextManager<String> CLEARABLE = new ClearableDummyContextManager();

    @BeforeEach
    @AfterEach
    void clear() {
        CLEARABLE.clear();
    }

    @Test
    void testClearActiveContexts_byManager() {
        assertThat(CLEARABLE).isInstanceOf(ContextManager.class);

        try (Context first = CLEARABLE.initializeNewContext("First value")) {
            try (Context second = CLEARABLE.initializeNewContext("Second value")) {
                try (Context third = CLEARABLE.initializeNewContext("Third value")) {
                    assertThat(CLEARABLE.getActiveContextValue()).isEqualTo("Third value");
                    ContextManager.clearAll();
                    assertThat(CLEARABLE.getActiveContextValue()).isNull();
                }
                // third.close() should have restored second.
                assertThat(CLEARABLE.getActiveContextValue()).isEqualTo("Second value");
                ContextManager.clearAll();
                assertThat(CLEARABLE.getActiveContextValue()).isNull();
            }
            // second.close() should have restored first.
            assertThat(CLEARABLE.getActiveContextValue()).isEqualTo("First value");
            ContextManager.clearAll();
            assertThat(CLEARABLE.getActiveContextValue()).isNull();
        }
        assertThat(CLEARABLE.getActiveContextValue()).isNull();
    }

    @Test
    void testClearActiveContexts_exception() {
        ThrowingContextManager.onGet = new IllegalStateException("Cannot get the current active context!");
        Executable callClearAll = ContextManager::clearAll;
        assertDoesNotThrow(callClearAll);
    }
}
