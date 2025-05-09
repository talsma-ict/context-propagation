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

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for the {@link Slf4jMdcManager}.
 *
 * @author Sjoerd Talsma
 */
public class Slf4jMdcManagerTest {

    ExecutorService threadpool;

    @BeforeEach
    public void setupThreadpool() {
        threadpool = new ContextAwareExecutorService(Executors.newCachedThreadPool());
    }

    @AfterEach
    public void shutdownThreadpool() {
        threadpool.shutdown();
    }

    @BeforeEach
    @AfterEach
    public void clearMDC() {
        MDC.clear();
    }

    private static final Callable<String> GET_MDC_ITEM = new Callable<String>() {
        public String call() {
            return MDC.get("mdc-item");
        }
    };

    @Test
    public void testMdcItemPropagation() throws ExecutionException, InterruptedException {
        MDC.put("mdc-item", "Item value");
        Future<String> itemValue = threadpool.submit(GET_MDC_ITEM);
        assertThat(itemValue.get(), is("Item value"));
    }

    @Test
    public void testMdcItemRestoration() throws Exception {
        MDC.put("mdc-item", "Value 1");

        ContextSnapshot snapshot = ContextSnapshot.capture();
        assertThat("New snapshot shouldn't manipulate MDC.", GET_MDC_ITEM.call(), is("Value 1"));

        MDC.put("mdc-item", "Value 2");
        assertThat("Sanity check: MDC changed?", GET_MDC_ITEM.call(), is("Value 2"));

        Closeable reactivation = snapshot.reactivate();
        assertThat("MDC changed by reactivation", GET_MDC_ITEM.call(), is("Value 1"));

        reactivation.close();
        assertThat("MDC restored?", GET_MDC_ITEM.call(), is("Value 2"));
    }

    @Test
    public void testSlf4jMdcManagerToString() {
        assertThat(Slf4jMdcManager.provider(), hasToString("Slf4jMdcManager"));
    }

    @Test
    public void testSlf4jMdcContextToString() {
        Slf4jMdcManager mgr = Slf4jMdcManager.provider();
        MDC.put("dummy", "value");
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        assertThat(mgr.getActiveContextValue(), equalTo(mdc));

        try (Context ctx = mgr.initializeNewContext(mdc)) {
            assertThat(ctx, hasToString("Slf4jMdcContext" + mdc));
        }
    }

    @Test
    public void testClearActiveContexts() {
        MDC.put("dummy", "value");
        // Test no-op for MdcManager
        ContextManager.clearAll();
        assertThat(MDC.get("dummy"), is("value"));
    }
}
