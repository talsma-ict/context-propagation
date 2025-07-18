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
package nl.talsmasoftware.context.timers.opentracing;

import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.GlobalTracerTestUtil;
import nl.talsmasoftware.context.api.ContextSnapshot;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test and demonstration of the tracing of context switches themselves.
 *
 * @author Sjoerd Talsma
 */
class OpentracingContextTimerTest {

    static final String PROPERTY_NAME = "opentracing.trace.contextmanager";
    static final String OLD_SYSTEM_PROPERTY = System.getProperty(PROPERTY_NAME);

    static MockTracer tracer = new MockTracer();

    @BeforeAll
    static void initGlobalTracer() {
        GlobalTracerTestUtil.resetGlobalTracer();
        GlobalTracer.registerIfAbsent(tracer);
    }

    @AfterAll
    static void resetGlobalTracer() {
        GlobalTracerTestUtil.resetGlobalTracer();
    }

    @BeforeEach
    void setup() {
        System.clearProperty(PROPERTY_NAME);
        tracer.reset();
    }

    @AfterEach
    void restoreSystemproperty() {
        if (OLD_SYSTEM_PROPERTY == null) System.clearProperty(PROPERTY_NAME);
        else System.setProperty(PROPERTY_NAME, OLD_SYSTEM_PROPERTY);
    }

    @Test
    void testDisabledByDefault() {
        ContextSnapshot.capture().reactivate().close();
        assertThat(tracer.finishedSpans()).isEmpty();
    }

    @Test
    void testTraceCaptureContextSnapshot() {
        System.setProperty(PROPERTY_NAME, "true");
        ContextSnapshot.capture();
        assertThat(tracer.finishedSpans())
                .anyMatch(span -> "ContextSnapshot.capture".equals(span.operationName()));
    }

    @Test
    void testTraceReactivateContextSnapshot() {
        System.setProperty(PROPERTY_NAME, "true");
        ContextSnapshot.capture().reactivate().close();
        assertThat(tracer.finishedSpans())
                .anyMatch(span -> "ContextSnapshot.reactivate".equals(span.operationName()));
    }

}
