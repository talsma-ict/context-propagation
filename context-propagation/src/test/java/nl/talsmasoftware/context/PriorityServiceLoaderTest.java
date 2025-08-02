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
package nl.talsmasoftware.context;

import org.junit.jupiter.api.Test;

import javax.annotation.Priority;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for the PriorityServiceLoader class.
 *
 * @author Sjoerd Talsma
 */
public class PriorityServiceLoaderTest {

    @Test
    public void testWithoutServiceType() {
        try {
            new PriorityServiceLoader<Object>(null);
            fail("Exception expected");
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), is(notNullValue()));
        }
    }

    @Test
    public void testMultipleIterations() {
        List<ContextManager<?>> managers = new ArrayList<ContextManager<?>>();
        PriorityServiceLoader<ContextManager> subject = new PriorityServiceLoader<ContextManager>(ContextManager.class);
        for (ContextManager<?> mgr : subject) assertThat(mgr, is(notNullValue()));
        for (ContextManager<?> mgr : subject) managers.add(mgr);

        assertThat(managers, hasItem(new DummyContextManager()));
    }

    @Test
    public void testUnimplementedService() {
        Collection<UnimplementedService> found = new ArrayList<UnimplementedService>();
        for (UnimplementedService svc : new PriorityServiceLoader<UnimplementedService>(UnimplementedService.class)) {
            found.add(svc);
        }
        assertThat(found, is(empty()));
    }

    @Test
    public void testSingleService() {
        assertThat(new PriorityServiceLoader<ServiceWithSingleImplementation>(ServiceWithSingleImplementation.class), contains(
                instanceOf(SingleImplementationWithLowestPriority.class)
        ));
    }

    @Test
    public void testPrioritization() {
        assertThat(new PriorityServiceLoader<TestService>(TestService.class), contains(
                instanceOf(HighestPrioImplementation.class),
                instanceOf(ImplementationWithoutPriority.class),
                instanceOf(LowestPrioImplementation.class)
        ));
    }

    interface UnimplementedService {
    }

    interface ServiceWithSingleImplementation {
    }

    interface TestService {
    }

    @Priority(Integer.MIN_VALUE)
    public static class LowestPrioImplementation implements TestService {

    }

    @Priority(0)
    public static class HighestPrioImplementation implements TestService {

    }

    public static class ImplementationWithoutPriority implements TestService {

    }

    @Priority(Integer.MIN_VALUE)
    public static class SingleImplementationWithLowestPriority implements ServiceWithSingleImplementation {

    }
}
