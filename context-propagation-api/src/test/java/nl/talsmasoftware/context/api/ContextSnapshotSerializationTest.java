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
package nl.talsmasoftware.context.api;

import nl.talsmasoftware.context.dummy.DummyContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ContextSnapshotSerializationTest {
    DummyContextManager dummyManager = new DummyContextManager();

    @BeforeEach
    @AfterEach
    void clear() {
        dummyManager.clear();
    }

    @Test
    void contextSnapshotMustBeSerializable() {
        String randomValue = "Dummy-" + UUID.randomUUID();
        ContextSnapshot snapshot;
        try (Context ignored = dummyManager.activate(randomValue)) {
            snapshot = ContextSnapshot.capture();
        }
        assertThat(dummyManager.getActiveContextValue()).isNull();
        assertThat(snapshot.getCapturedValue(dummyManager)).isEqualTo(randomValue);

        byte[] serialized = assertDoesNotThrow(() -> serialize(snapshot));
        assertThat(serialized).isNotNull().isNotEmpty();

        ContextSnapshot deserialized = assertDoesNotThrow(() -> deserialize(serialized));
        assertThat(deserialized).isNotNull().isNotSameAs(snapshot);

        assertThat(deserialized.getCapturedValue(dummyManager)).isEqualTo(randomValue);
        try (ContextSnapshot.Reactivation ignored = deserialized.reactivate()) {
            assertThat(dummyManager.getActiveContextValue()).isEqualTo(randomValue);
        }
    }

    static byte[] serialize(ContextSnapshot snapshot) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            new ObjectOutputStream(buf).writeObject(snapshot);
            return buf.toByteArray();
        } catch (IOException e) {
            throw new AssertionError("Unexpected exception while serializing " + snapshot, e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T deserialize(byte[] bytes) {
        try {
            return (T) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new AssertionError("Unexpected exception while deserializing object.", e);
        }
    }
}
