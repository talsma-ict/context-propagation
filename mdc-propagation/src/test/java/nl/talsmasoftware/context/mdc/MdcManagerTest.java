/*
 * Copyright 2016-2017 Talsma ICT
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
package nl.talsmasoftware.context.mdc;

import nl.talsmasoftware.context.executors.ContextAwareExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for the {@link MdcManager}.
 *
 * @author Sjoerd Talsma
 */
public class MdcManagerTest {

    ExecutorService threadpool;

    @Before
    public void setupThreadpool() {
        threadpool = new ContextAwareExecutorService(Executors.newCachedThreadPool());
    }

    @After
    public void shutdownThreadpool() {
        threadpool.shutdown();
        threadpool = null;
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

}
