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
package nl.talsmasoftware.context.timers.opentelemetry;

import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.api.ContextSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

class OpenTelemetryContextTimerTest {
    @RegisterExtension
    static final OpenTelemetryExtension TELEMETRY = OpenTelemetryExtension.create();

    OpenTelemetryContextTimer subject = new OpenTelemetryContextTimer();

    @Test
    void update_ContextSnapshot_capture() {
        // prepare

        // execute
        subject.update(ContextSnapshot.class, "capture", 5, TimeUnit.MILLISECONDS, null);

        // verify
        TELEMETRY.assertTraces().hasTracesSatisfyingExactly(trace ->
                trace.hasSpansSatisfyingExactly(span -> span.hasName("ContextSnapshot.capture")));
    }

    @Test
    void update_ContextSnapshot_reactivate() {
        // prepare

        // execute
        subject.update(ContextSnapshot.class, "reactivate", 5, TimeUnit.MILLISECONDS, null);

        // verify
        TELEMETRY.assertTraces().hasTracesSatisfyingExactly(trace ->
                trace.hasSpansSatisfyingExactly(span -> span.hasName("ContextSnapshot.reactivate")));
    }

    @Test
    void update_ContextManager_initializeNewContext_not_traced() {
        // prepare
        ContextManager<String> contextManagerMock = mock(ContextManager.class);

        // execute
        subject.update(contextManagerMock.getClass(), "initializeNewContext", 5, TimeUnit.MILLISECONDS, null);

        // verify
        TELEMETRY.assertTraces().isEmpty();
    }

    @Test
    void update_ContextSnapshot_reactivate_error() {
        // prepare
        RuntimeException error = new RuntimeException("Whoops!");

        // execute
        subject.update(ContextSnapshot.class, "reactivate", 5, TimeUnit.MILLISECONDS, error);

        // verify
        TELEMETRY.assertTraces().hasTracesSatisfyingExactly(trace ->
                trace.hasSpansSatisfyingExactly(span ->
                        span.hasName("ContextSnapshot.reactivate")
                                .hasEventsSatisfyingExactly(event -> event.hasName("exception"))));
    }

}
