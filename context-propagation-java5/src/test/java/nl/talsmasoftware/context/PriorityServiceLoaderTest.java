/*
 * Copyright 2016-2018 Talsma ICT
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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;

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
    public void testReloadBeforeIteration() {
        List<ContextManager<?>> managers = new ArrayList<ContextManager<?>>();
        PriorityServiceLoader<ContextManager> subject = new PriorityServiceLoader<ContextManager>(ContextManager.class);
        subject.reload();
        for (ContextManager<?> mgr : subject) managers.add(mgr);

        assertThat(managers, hasItem(new DummyContextManager()));
    }

    @Test
    public void testReloadDuringIteration() {
        List<ContextManager<?>> managers = new ArrayList<ContextManager<?>>();
        PriorityServiceLoader<ContextManager> subject = new PriorityServiceLoader<ContextManager>(ContextManager.class);
        for (ContextManager<?> mgr : subject) {
            subject.reload();
            managers.add(mgr);
            subject.reload();
        }

        assertThat(managers, hasItem(new DummyContextManager()));
    }

    @Test
    public void testReloadAfterIteration() {
        List<ContextManager<?>> managers = new ArrayList<ContextManager<?>>();
        PriorityServiceLoader<ContextManager> subject = new PriorityServiceLoader<ContextManager>(ContextManager.class);
        for (ContextManager<?> mgr : subject) managers.add(mgr);
        subject.reload();

        assertThat(managers, hasItem(new DummyContextManager()));
    }

    @Test
    public void testMultipleIterations() {
        List<ContextManager<?>> managers = new ArrayList<ContextManager<?>>();
        PriorityServiceLoader<ContextManager> subject = new PriorityServiceLoader<ContextManager>(ContextManager.class);
        for (ContextManager<?> mgr : subject) assertThat(mgr, is(notNullValue()));
        for (ContextManager<?> mgr : subject) managers.add(mgr);

        assertThat(managers, hasItem(new DummyContextManager()));
    }

}
