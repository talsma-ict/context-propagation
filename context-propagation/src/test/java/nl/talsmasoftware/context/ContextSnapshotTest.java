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
package nl.talsmasoftware.context;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextSnapshot;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;

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
        Context<String> ctx = MGR.initializeNewContext("Dummy value");
        try {
            assertThat(ContextManagers.createContextSnapshot(), hasToString(startsWith("ContextSnapshot{size=")));
        } finally {
            ctx.close();
        }
    }

    @Test
    public void testSnapshotReactivate() throws IOException {
        Context<String> ctx = MGR.initializeNewContext("Old value");
        try {
            ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
            Context<String> ctx2 = MGR.initializeNewContext("New value");
            try {

                assertThat(MGR.getActiveContext().getValue(), is("New value"));
                Closeable reactivation = snapshot.reactivate();
                try {
                    assertThat(MGR.getActiveContext().getValue(), is("Old value"));
                    assertThat(reactivation, hasToString(startsWith("ReactivatedContext{size=")));
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
