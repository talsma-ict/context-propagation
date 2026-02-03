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
package nl.talsmasoftware.context.api;

import nl.talsmasoftware.context.dummy.DummyContextManager;
import nl.talsmasoftware.context.dummy.ThrowingContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Isolated("This test manipulates the service cache.")
class ContextSnapshotSerializationTest {
    DummyContextManager dummyManager = new DummyContextManager();
    ThrowingContextManager throwingManager = new ThrowingContextManager();
    ConcurrentMap<Class, List> cache = ServiceCacheTestUtil.getInternalCacheMap();

    @BeforeEach
    @AfterEach
    void clear() {
        dummyManager.clear();
        ServiceCache.clear();
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

    @Test
    void nonSerializableContextValuesAreSkipped() {
        String dummyValue = "Dummy-" + UUID.randomUUID();
        Object nonSerializable = new Object();
        dummyManager.activate(dummyValue);
        throwingManager.activate(nonSerializable);
        ContextSnapshot snapshot = ContextSnapshot.capture();
        ContextManager.clearAll();

        ContextSnapshot deserialized = assertDoesNotThrow(() -> deserialize(serialize(snapshot)));
        try (ContextSnapshot.Reactivation ignored = deserialized.reactivate()) {
            assertThat(dummyManager.getActiveContextValue()).isEqualTo(dummyValue);
            assertThat(throwingManager.getActiveContextValue()).isNull();
        }
    }

    @Test
    void missingContextManagerOnDeserializationIsSkipped() {
        String dummyValue = "Dummy-" + UUID.randomUUID();
        dummyManager.activate(dummyValue);
        byte[] snapshot = serialize(ContextSnapshot.capture());
        ContextManager.clearAll();

        cache.put(ContextManager.class, emptyList());
        ContextSnapshot deserialized = assertDoesNotThrow(() -> deserialize(snapshot));
        try (ContextSnapshot.Reactivation ignored = deserialized.reactivate()) {
            assertThat(dummyManager.getActiveContextValue()).isNull();
        }

        assertThatThrownBy(() -> deserialized.getCapturedValue(dummyManager))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deserializingCorruptedDataIsCaught() {
        byte[] serialized = createSerialized(
                new String[]{DummyContextManager.class.getName(), ThrowingContextManager.class.getName()},
                new Serializable[]{"Single value!"});

        assertThatThrownBy(() -> deserialize(serialized))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Serialized ContextSnapshot has mismatched number of context managers and values.");
    }

    static byte[] serialize(Object snapshot) {
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(buf)) {
            out.writeObject(snapshot);
            return buf.toByteArray();
        } catch (IOException e) {
            throw new AssertionError("Unexpected exception while serializing " + snapshot, e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T deserialize(byte[] bytes) {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new AssertionError("Unexpected exception while deserializing object.", e);
        }
    }

    static byte[] createSerialized(String[] managerNames, Serializable[] values) {
        try {
            Constructor<?> constructor = Class.forName(ContextSnapshotImpl.class.getName() + "$Serialized")
                    .getDeclaredConstructor(String[].class, Serializable[].class);
            constructor.setAccessible(true);
            return serialize(constructor.newInstance(managerNames, values));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unexpected exception while creating serialized object.", e);
        }
    }
}
