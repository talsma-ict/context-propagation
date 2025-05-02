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
package nl.talsmasoftware.context.core;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.dummy.DummyContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NoContextManagersTest {
    private static final String SERVICE_LOCATION = "target/test-classes/META-INF/services/";
    private static final File SERVICE_FILE = new File(SERVICE_LOCATION + ContextManager.class.getName());
    private static final File TMP_SERVICE_FILE = new File(SERVICE_LOCATION + "tmp-ContextManager");

    @BeforeEach
    void avoidContextManagersCache() {
        ContextManager.useClassLoader(new ClassLoader(Thread.currentThread().getContextClassLoader()) {
        });
        assertThat(SERVICE_FILE.renameTo(TMP_SERVICE_FILE)).as("Service file moved").isTrue();
    }

    @AfterEach
    void resetDefaultClassLoader() {
        ContextManager.useClassLoader(null);
        assertThat(TMP_SERVICE_FILE.renameTo(SERVICE_FILE)).as("Service file restored").isTrue();
    }

    @Test
    void testReactivate_withoutContextManagers() {
        Context ctx1 = new DummyContext("foo");
        ContextSnapshot snapshot = ContextSnapshot.capture();
        assertThat(DummyContext.currentValue()).isEqualTo("foo");
        ctx1.close();
        assertThat(DummyContext.currentValue()).isNull();

        ContextSnapshot.Reactivation reactivated = snapshot.reactivate();
        assertThat(DummyContext.currentValue()).isNull();
        reactivated.close();
        assertThat(DummyContext.currentValue()).isNull();
    }

    @Test
    void testCreateSnapshot_withoutContextManagers() {
        ContextSnapshot snapshot = ContextSnapshot.capture();
        assertThat(snapshot).isNotNull();

        ContextSnapshot.Reactivation reactivated = snapshot.reactivate();
        assertThat(reactivated).isNotNull();
        reactivated.close();
    }

    @Test
    void testClearManagedContexts_withoutContextManagers() {
        Executable executable = ContextManager::clearAll;
        assertDoesNotThrow(executable);
    }

}
