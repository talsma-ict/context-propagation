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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Sjoerd Talsma
 */
public class ContextSnapshotTest {
    private static final DummyContextManager MGR = new DummyContextManager();

    @Test
    public void testSnapshotToString() {
        Context<String> ctx = MGR.initializeNewContext("Dummy value");
        try {
            assertThat(ContextManagers.createContextSnapshot(), hasToString("ContextSnapshot{size=1}"));
        } finally {
            ctx.close();
        }
    }

    @Test
    public void testSnapshotReactivate() {
        Context<String> ctx = MGR.initializeNewContext("Old value");
        try {
            ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
            Context<String> ctx2 = MGR.initializeNewContext("New value");
            try {

                assertThat(MGR.getActiveContext().getValue(), is("New value"));
                Context<Void> reactivation = snapshot.reactivate();
                try {
                    assertThat(MGR.getActiveContext().getValue(), is("Old value"));
                    assertThat(reactivation.getValue(), is(nullValue()));
                    assertThat(reactivation, hasToString("ReactivatedContext{size=1}"));
                } finally {
                    reactivation.close();
                }
                assertThat(MGR.getActiveContext().getValue(), is("New value"));

            } finally {
                ctx2.close();
            }
        } finally {
            ctx.close();
        }
    }

}
