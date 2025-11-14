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
package nl.talsmasoftware.context.api;

import nl.talsmasoftware.context.dummy.DummyContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Isolated("Service cache is manipulated during this test")
class NoContextManagersTest {
    @SuppressWarnings("rawtypes")
    private static final ConcurrentMap<Class, List> SERVICE_CACHE = ServiceCacheTestUtil.getInternalCacheMap();

    @BeforeEach
    void avoidContextManagersCache() {
        SERVICE_CACHE.put(ContextManager.class, Collections.emptyList());
    }

    @AfterEach
    void resetDefaultClassLoader() {
        SERVICE_CACHE.clear();
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
    void testCaptureSnapshot_withoutContextManagers() {
        ContextSnapshot snapshot = ContextSnapshot.capture();
        assertThat(snapshot).isNotNull().hasToString("ContextSnapshot{size=0}");

        ContextSnapshot.Reactivation reactivated = snapshot.reactivate();
        assertThat(reactivated).isNotNull();
        reactivated.close();
        assertThat(SERVICE_CACHE).as("Service cache after failed capture").doesNotContainKey(ContextManager.class);
    }

    @Test
    void captureSnapshot_withDebugLogging() {
        Logger.getLogger(ContextSnapshot.class.getName()).setLevel(java.util.logging.Level.FINEST);

        ContextSnapshot result = assertDoesNotThrow(ContextSnapshot::capture);

        assertThat(result).isNotNull().hasToString("ContextSnapshot{size=0}");
        assertThat(SERVICE_CACHE).as("Service cache after failed capture").doesNotContainKey(ContextManager.class);
    }

    @Test
    void testClearAllContextManagers_withoutContextManagers() {
        Executable executable = ContextManager::clearAll;
        assertDoesNotThrow(executable);
    }

}
