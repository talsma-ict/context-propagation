/*
 * Copyright 2016-2022 Talsma ICT
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
package nl.talsmasoftware.context.opentracing;

import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.GlobalTracerTestUtil;
import nl.talsmasoftware.context.ContextManagers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static nl.talsmasoftware.context.opentracing.MockSpanMatcher.withOperationName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

/**
 * Test and demonstration of the tracing of context switches themselves.
 *
 * @author Sjoerd Talsma
 */
public class OpentracingContextTimerTest {

    private static final String PROPERTY_NAME = "opentracing.trace.contextmanager";
    private static final String oldSystemProperty = System.getProperty(PROPERTY_NAME);

    static MockTracer tracer = new MockTracer();

    @BeforeAll
    public static void initGlobalTracer() {
        GlobalTracerTestUtil.resetGlobalTracer();
        GlobalTracer.register(tracer);
    }

    @AfterAll
    public static void resetGlobalTracer() {
        GlobalTracerTestUtil.resetGlobalTracer();
    }

    @BeforeEach
    public void setup() {
        System.clearProperty(PROPERTY_NAME);
        tracer.reset();
    }

    @AfterEach
    public void restoreSystemproperty() {
        if (oldSystemProperty == null) System.clearProperty(PROPERTY_NAME);
        else System.setProperty(PROPERTY_NAME, oldSystemProperty);
    }

    @Test
    public void testDisabledByDefault() {
        ContextManagers.createContextSnapshot().reactivate().close();

        assertThat(tracer.finishedSpans(), is(empty()));
    }

    @Test
    public void testTraceCreateContextSnapshot() {
        System.setProperty(PROPERTY_NAME, "true");
        ContextManagers.createContextSnapshot();
        assertThat(tracer.finishedSpans(), hasItem(withOperationName("ContextManagers.createContextSnapshot")));
    }

    @Test
    public void testTraceReactivateContextSnapshot() {
        System.setProperty(PROPERTY_NAME, "true");
        ContextManagers.createContextSnapshot().reactivate().close();
        assertThat(tracer.finishedSpans(), hasItem(withOperationName("ContextSnapshot.reactivate")));
    }

}
