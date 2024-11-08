/*
 * Copyright 2016-2024 Talsma ICT
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
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.dummy.DummyContextManager;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

/**
 * @author Sjoerd Talsma
 */
public class ContextSnapshotTest {
    private static final DummyContextManager MGR = new DummyContextManager();

    @Test
    public void testSnapshotToString() {
        assertThat(ContextManagers.createContextSnapshot(), hasToString(startsWith("ContextSnapshot{size=")));
    }

    @Test
    public void testSnapshotReactivate() {
        try (Context<String> ctx = MGR.initializeNewContext("Old value")) {
            ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
            try (Context<String> ctx2 = MGR.initializeNewContext("New value")) {
                assertThat(MGR.getActiveContextValue(), is("New value"));

                try (ContextSnapshot.Reactivation reactivation = snapshot.reactivate()) {
                    assertThat(MGR.getActiveContextValue(), is("Old value"));
                    assertThat(reactivation, hasToString(startsWith("ContextSnapshot.Reactivation{size=")));
                }

                assertThat(MGR.getActiveContextValue(), is("New value"));
            }
        }
    }

}
