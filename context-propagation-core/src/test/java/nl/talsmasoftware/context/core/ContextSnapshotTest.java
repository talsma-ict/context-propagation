/*
 * Copyright 2016-2026 Talsma ICT
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sjoerd Talsma
 */
class ContextSnapshotTest {
    private static final DummyContextManager MGR = new DummyContextManager();

    @Test
    void testSnapshotToString() {
        assertThat(ContextSnapshot.capture().toString()).startsWith("ContextSnapshot{size=");
    }

    @Test
    void testSnapshotReactivate() {
        try (Context ignored = MGR.activate("Old value")) {
            ContextSnapshot snapshot = ContextSnapshot.capture();
            try (Context ignored2 = MGR.activate("New value")) {
                assertThat(MGR.getActiveContextValue()).isEqualTo("New value");

                try (ContextSnapshot.Reactivation reactivation = snapshot.reactivate()) {
                    assertThat(MGR.getActiveContextValue()).isEqualTo("Old value");
                    assertThat(reactivation.toString()).startsWith("ContextSnapshot.Reactivation{size=");
                }

                assertThat(MGR.getActiveContextValue()).isEqualTo("New value");
            }
        }
    }

}
