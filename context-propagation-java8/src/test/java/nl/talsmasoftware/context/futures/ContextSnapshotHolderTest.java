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
package nl.talsmasoftware.context.futures;

import nl.talsmasoftware.context.ContextSnapshot;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;

public class ContextSnapshotHolderTest {

    @Test
    public void testCreateWithNull() {
        assertThat(new ContextSnapshotHolder(null).get(), is(notNullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void testAcceptNull() {
        new ContextSnapshotHolder(mock(ContextSnapshot.class)).accept(null);
    }

    @Test
    public void testGet() {
        ContextSnapshot snapshot1 = mock(ContextSnapshot.class);
        ContextSnapshot snapshot2 = mock(ContextSnapshot.class);

        ContextSnapshotHolder holder = new ContextSnapshotHolder(snapshot1);
        assertThat(holder.get(), is(sameInstance(snapshot1)));

        holder.accept(snapshot2);
        assertThat(holder.get(), is(sameInstance(snapshot2)));
    }
}
