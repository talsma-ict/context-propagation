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
package nl.talsmasoftware.context.core.delegation;

import nl.talsmasoftware.context.api.ContextSnapshot;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * @author Sjoerd Talsma
 */
class WrapperWithContextTest {

    @Test
    void testCreateWrapperWithoutDelegate() {
        WrapperWithContext<Object> wrapper = new WrapperWithContext<Object>(mock(ContextSnapshot.class), null) {
        };
        assertThat(wrapper).isNotNull();
        assertThat(wrapper.delegate()).isNull();
    }

    @Test
    void testCreateWrapperWithoutSnapshot() {
        ContextSnapshot snapshot = null;
        Wrapper<Object> wrapper = mock(Wrapper.class);
        Exception expected = assertThrows(NullPointerException.class, () ->
                new WrapperWithContext<Object>(snapshot, wrapper) {
                });
        assertThat(expected.toString()).contains("No context snapshot provided");
    }

    @Test
    void testCreateWrapperWithoutSnapshotSupplier() {
        Supplier<ContextSnapshot> supplier = null;
        Wrapper<Object> wrapper = mock(Wrapper.class);
        Exception expected = assertThrows(NullPointerException.class, () ->
                new WrapperWithContext<Object>(supplier, wrapper) {
                });
        assertThat(expected.toString()).contains("No context snapshot supplier provided");
    }

    @Test
    void testSupplySnapshotNull() {
        WrapperWithContext<Object> wrapperWithContext = new WrapperWithContext<Object>((Supplier<ContextSnapshot>) () -> null, mock(Wrapper.class)) {
        };
        Exception expected = assertThrows(NullPointerException.class, wrapperWithContext::snapshot);
        assertThat(expected.toString()).contains("Context snapshot is <null>");
    }

    @Test
    void testEqualsHashcode() {
        Set<WrapperWithContext<Object>> set = new LinkedHashSet<>();
        ContextSnapshot snapshot = mock(ContextSnapshot.class);
        Wrapper<Object> delegate = mock(Wrapper.class);

        WrapperWithContext<Object> wrapper = new DoNothingWrapper(snapshot, delegate);
        assertThat(set.add(wrapper)).isTrue();
        assertThat(set.add(wrapper)).isFalse();
        assertThat(set.add(new WrapperWithContext<Object>(mock(ContextSnapshot.class), delegate) {
        })).isTrue();
        assertThat(set.add(new WrapperWithContext<Object>(snapshot, mock(Wrapper.class)) {
        })).isTrue();
        assertThat(set).hasSize(3);
        WrapperWithContext<Object> copy = new WrapperWithContext<Object>(snapshot, delegate) {
        };
        assertThat(wrapper)
                .isEqualTo(wrapper)
                .isEqualTo(new DoNothingWrapper(snapshot, delegate))
                .isNotEqualTo(copy); // different inner class
    }

    static class DoNothingWrapper extends WrapperWithContext<Object> {
        protected DoNothingWrapper(ContextSnapshot snapshot, Object delegate) {
            super(snapshot, delegate);
        }
    }

    @Test
    void testToString() {
        ContextSnapshot snapshot = mock(ContextSnapshot.class);
        Wrapper<Object> delegate = mock(Wrapper.class);
        WrapperWithContext<Object> wrapper = new WrapperWithContext<Object>(snapshot, delegate) {
        };

        assertThat(wrapper.toString()).contains(snapshot.toString());
        assertThat(wrapper.toString()).contains(wrapper.toString());
    }

    @Test
    void testToString_contextSnapshotSupplier() {
        final ContextSnapshot snapshot = mock(ContextSnapshot.class);
        final Wrapper<Object> delegate = mock(Wrapper.class);
        final WrapperWithContext<Object> wrapper = new WrapperWithContext<Object>(() -> snapshot, delegate) {
        };

        // Test that toString does NOT trigger eager snapshot evaluation.
        assertThat(wrapper.toString())
                .doesNotContain(snapshot.toString())
                .contains(wrapper.toString());
        wrapper.snapshot();
        assertThat(wrapper.toString()).contains(snapshot.toString());
    }
}
