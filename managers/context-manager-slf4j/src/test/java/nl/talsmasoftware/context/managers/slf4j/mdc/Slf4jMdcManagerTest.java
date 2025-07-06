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
package nl.talsmasoftware.context.managers.slf4j.mdc;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.core.concurrent.ContextAwareExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for the {@link Slf4jMdcManager}.
 *
 * @author Sjoerd Talsma
 */
class Slf4jMdcManagerTest {

    ExecutorService threadpool;

    @BeforeEach
    void setupThreadpool() {
        threadpool = ContextAwareExecutorService.wrap(Executors.newCachedThreadPool());
    }

    @AfterEach
    void shutdownThreadpool() {
        threadpool.shutdown();
    }

    @BeforeEach
    @AfterEach
    void clearMDC() {
        MDC.clear();
    }

    @Test
    void testMdcItemPropagation() throws ExecutionException, InterruptedException {
        MDC.put("mdc-item", "Item value");
        Future<String> itemValue = threadpool.submit(() -> MDC.get("mdc-item"));
        assertThat(itemValue.get()).isEqualTo("Item value");
    }

    @Test
    void testMdcItemRestoration() throws Exception {
        MDC.put("mdc-item", "Value 1");

        ContextSnapshot snapshot = ContextSnapshot.capture();
        assertThat(MDC.get("mdc-item")).withFailMessage("New snapshot shouldn't manipulate MDC.").isEqualTo("Value 1");

        MDC.put("mdc-item", "Value 2");
        assertThat(MDC.get("mdc-item")).as("Sanity check: MDC changed?").isEqualTo("Value 2");

        try (ContextSnapshot.Reactivation reactivation = snapshot.reactivate()) {
            assertThat(MDC.get("mdc-item")).withFailMessage("MDC changed by reactivation").isEqualTo("Value 1");
        }

        assertThat(MDC.get("mdc-item")).as("MDC restored?").isEqualTo("Value 2");
    }

    @Test
    void testSlf4jMdcManagerToString() {
        assertThat(Slf4jMdcManager.provider()).hasToString("Slf4jMdcManager");
    }

    @Test
    void testSlf4jMdcContextToString() {
        Slf4jMdcManager manager = Slf4jMdcManager.provider();
        try (Context context = manager.activate(MDC.getCopyOfContextMap())) {
            assertThat(manager).hasToString("Slf4jMdcManager");
            assertThat(context).hasToString("Slf4jMdcContext");
        }
    }

    @Test
    void testClearActiveContexts() {
        MDC.put("dummy", "value");
        // Test no-op for MdcManager
        ContextManager.clearAll();
        assertThat(MDC.get("dummy")).isEqualTo("value");
    }

    @Test
    void unrelatedKeysAreNotTouched() throws Exception {
        MDC.put("test-key-1", "value");
        ContextSnapshot snapshot = ContextSnapshot.capture();

        MDC.remove("test-key-1");
        MDC.put("test-key-2", "unrelated value not in the snapshot");
        assertThat(snapshot.wrap(() -> MDC.get("test-key-1")).call()).isEqualTo("value");
        assertThat(snapshot.wrap(() -> MDC.get("test-key-2")).call()).isEqualTo("unrelated value not in the snapshot");

        assertThat(MDC.get("test-key-1")).isNull();
        assertThat(MDC.get("test-key-2")).isEqualTo("unrelated value not in the snapshot");
    }
}
