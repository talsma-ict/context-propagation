/*
 * Copyright 2016-2019 Talsma ICT
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
package nl.talsmasoftware.context.observer;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.DummyContextManager;
import org.junit.Test;

import static nl.talsmasoftware.context.observer.Observed.activated;
import static nl.talsmasoftware.context.observer.Observed.deactivated;
import static nl.talsmasoftware.context.observer.ThrowingContextObserver.observed;
import static nl.talsmasoftware.context.observer.ThrowingContextObserver.observedContextManager;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test for context observers that throw exceptions.
 */
public class ThrowingContextObserverTest {
    private static DummyContextManager MGR = new DummyContextManager();

    @Test
    public void testExceptionDoesNotPreventFunctionality() {
        observedContextManager = MGR.getClass();
        observed.clear();
        try {

            Context<String> ctx = MGR.initializeNewContext("Some value");
            try {

                assertThat(MGR.getActiveContext().getValue(), is("Some value"));
                assertThat(observed, hasItem(activated(is("Some value"))));
                assertThat(observed, not(hasItem(deactivated(is("Some value")))));

            } finally {
                ctx.close();
            }

            assertThat(MGR.getActiveContext(), is(nullValue()));
            assertThat(observed, hasItem(deactivated(is("Some value"))));

        } finally {
            observedContextManager = null;
        }
    }

}
