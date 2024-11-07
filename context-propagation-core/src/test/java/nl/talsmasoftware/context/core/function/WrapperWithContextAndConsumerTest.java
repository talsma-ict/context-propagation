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
package nl.talsmasoftware.context.core.function;

import nl.talsmasoftware.context.api.ContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Sjoerd Talsma
 */
public class WrapperWithContextAndConsumerTest {

    private ContextSnapshot snapshot;

    @BeforeEach
    public void setUp() {
        snapshot = mock(ContextSnapshot.class);
    }

    @AfterEach
    public void verifyMocks() {
        verifyNoMoreInteractions(snapshot);
    }

    @Test
    @Deprecated
    public void testDeprecatedConsumerMethod() {
        assertThat(new WrapperWithContextAndConsumer<String>(snapshot, "string", null) {
        }.contextSnapshotConsumer, is(nullValue()));

        final Consumer<ContextSnapshot> contextSnapshotConsumer = snapshot -> {
        };

        assertThat(new WrapperWithContextAndConsumer<String>(snapshot, "string", contextSnapshotConsumer) {
                }.contextSnapshotConsumer,
                is(sameInstance(contextSnapshotConsumer)));
    }

}
