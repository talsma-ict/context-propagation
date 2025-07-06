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
package nl.talsmasoftware.context.managers.log4j2.threadcontext;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Log4J2ThreadContextSnapshotTest {
    @BeforeEach
    @AfterEach
    void clearThreadContext() {
        ThreadContext.clearAll();
    }

    @Test
    void testCaptureFromCurrentThread() {
        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        Log4j2ThreadContextSnapshot snapshot = Log4j2ThreadContextSnapshot.captureFromCurrentThread();

        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("map1", "value1");
        expectedMap.put("map2", "value2");
        assertThat(snapshot.getContextMap()).isEqualTo(expectedMap);

        List<String> expectedStack = Arrays.asList("stack1", "stack2");
        assertThat(snapshot.getContextStack().asList()).isEqualTo(expectedStack);
    }

    @Test
    void testCaptureFromCurrentThread_empty() {
        assertThat(ThreadContext.getDepth()).isEqualTo(0);
        assertThat(ThreadContext.isEmpty()).isTrue();

        Log4j2ThreadContextSnapshot snapshot = Log4j2ThreadContextSnapshot.captureFromCurrentThread();
        assertThat(snapshot.getContextMap()).isEmpty();
        assertThat(snapshot.getContextStack()).isEmpty();

        // Applying empty snapshot should have no effect
        snapshot.applyToCurrentThread();
        assertThat(ThreadContext.getDepth()).isEqualTo(0);
        assertThat(ThreadContext.isEmpty()).isTrue();
    }

    @Test
    void testCaptureFromCurrentThread_empty_apply() {
        assertThat(ThreadContext.getDepth()).isEqualTo(0);
        assertThat(ThreadContext.isEmpty()).isTrue();

        Log4j2ThreadContextSnapshot snapshot = Log4j2ThreadContextSnapshot.captureFromCurrentThread();

        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        // Modification of ThreadContext should not have affected snapshot
        assertThat(snapshot.getContextMap()).isEmpty();
        assertThat(snapshot.getContextStack()).isEmpty();

        // Applying empty context on top of existing one should have no effect
        snapshot.applyToCurrentThread();

        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("map1", "value1");
        expectedMap.put("map2", "value2");
        assertThat(ThreadContext.getContext()).isEqualTo(expectedMap);

        List<String> expectedStack = Arrays.asList("stack1", "stack2");
        assertThat(ThreadContext.getImmutableStack().asList()).isEqualTo(expectedStack);
    }

    /**
     * Verify that {@link Log4j2ThreadContextSnapshot} is a snapshot and not affected
     * by subsequent modification of {@link ThreadContext}.
     */
    @Test
    void testCaptureFromCurrentThread_modification() {
        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        Log4j2ThreadContextSnapshot snapshot = Log4j2ThreadContextSnapshot.captureFromCurrentThread();

        // Should not affect snapshot
        ThreadContext.put("map3", "value3");
        ThreadContext.push("stack3");

        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("map1", "value1");
        expectedMap.put("map2", "value2");
        assertThat(snapshot.getContextMap()).isEqualTo(expectedMap);

        List<String> expectedStack = Arrays.asList("stack1", "stack2");
        assertThat(snapshot.getContextStack().asList()).isEqualTo(expectedStack);
    }

    @Test
    void testToString() {
        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        Log4j2ThreadContextSnapshot snapshot = Log4j2ThreadContextSnapshot.captureFromCurrentThread();
        String expectedString = "Log4j2ThreadContextSnapshot{contextMap=" + snapshot.getContextMap()
                + ", contextStack=" + snapshot.getContextStack() + "}";
        assertThat(snapshot).hasToString(expectedString);
    }

    /**
     * Verify that {@link Log4j2ThreadContextSnapshot#applyToCurrentThread()}
     * appends data to existing one, only overwriting existing one in case of conflict.
     */
    @Test
    void testApplyToCurrentThread() {
        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        Log4j2ThreadContextSnapshot snapshot = Log4j2ThreadContextSnapshot.captureFromCurrentThread();

        ThreadContext.clearAll();
        ThreadContext.put("map1", "old-value1");
        ThreadContext.put("map3", "old-value3");
        ThreadContext.push("stack3");

        snapshot.applyToCurrentThread();

        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("map1", "value1"); // old-value1 should have been overwritten
        expectedMap.put("map2", "value2");
        expectedMap.put("map3", "old-value3");
        assertThat(ThreadContext.getContext()).isEqualTo(expectedMap);

        List<String> expectedStack = Arrays.asList("stack3", "stack1", "stack2");
        assertThat(ThreadContext.getImmutableStack().asList()).isEqualTo(expectedStack);
    }

    @Test
    void testImmutableContextMap() {
        ThreadContext.put("key1", "value1");
        ThreadContext.put("key2", "value2");
        final Log4j2ThreadContextSnapshot snapshot = Log4j2ThreadContextSnapshot.captureFromCurrentThread();

        assertThatThrownBy(() -> snapshot.getContextMap().put("key3", "value3"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(snapshot.getContextMap()).hasSize(2);
    }

    @Test
    void testImmutableContextStack() {
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");
        final Log4j2ThreadContextSnapshot snapshot = Log4j2ThreadContextSnapshot.captureFromCurrentThread();

        assertThatThrownBy(() -> snapshot.getContextStack().pop())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.getContextStack().push("stack3"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(snapshot.getContextStack()).containsExactly("stack1", "stack2");
    }
}
