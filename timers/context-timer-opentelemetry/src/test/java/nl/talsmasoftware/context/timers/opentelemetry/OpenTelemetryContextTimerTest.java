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
import nl.talsmasoftware.context.api.ContextSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.TimeUnit;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

class OpenTelemetryContextTimerTest {
    @RegisterExtension
    static final OpenTelemetryExtension TELEMETRY = OpenTelemetryExtension.create();

    static final int HUNDRED_THOUSAND = 100000;

    OpenTelemetryContextTimer subject = new OpenTelemetryContextTimer();

    @Test
    void update_ContextSnapshot_capture() {
        // prepare

        // execute
        for (int i = 0; i < HUNDRED_THOUSAND; i++) {
            subject.update(ContextSnapshot.class, "capture", 5, TimeUnit.MILLISECONDS, null);
        }

        // verify
        assertThat(TELEMETRY.getMetrics())
                .singleElement()
                .satisfies(metric ->
                        assertThat(metric)
                                .hasName("ContextSnapshot.capture")
                                .hasDescription("Duration of ContextSnapshot.capture")
                                .hasUnit("milliseconds")
                                .hasHistogramSatisfying(histogram -> histogram
                                        .hasPointsSatisfying(point -> point
                                                .hasCount(HUNDRED_THOUSAND)
                                                .hasMin(5.0d)
                                                .hasMax(5.0d)
                                        )));
    }

    @Test
    void update_ContextSnapshot_reactivate() {
        // prepare

        // execute
        for (int i = 0; i < HUNDRED_THOUSAND; i++) {
            subject.update(ContextSnapshot.class, "reactivate", 5, TimeUnit.MILLISECONDS, null);
        }

        // verify
        assertThat(TELEMETRY.getMetrics())
                .singleElement()
                .satisfies(metric ->
                        assertThat(metric)
                                .hasName("ContextSnapshot.reactivate")
                                .hasDescription("Duration of ContextSnapshot.reactivate")
                                .hasUnit("milliseconds")
                                .hasHistogramSatisfying(histogram -> histogram
                                        .hasPointsSatisfying(point -> point
                                                .hasCount(HUNDRED_THOUSAND)
                                                .hasMin(5.0d)
                                                .hasMax(5.0d)
                                        )));
    }

    @Test
    void update_ContextSnapshot_reactivate_error() {
        // prepare
        RuntimeException error = new RuntimeException("Whoops!");

        // execute
        for (int i = 0; i < HUNDRED_THOUSAND; i++) {
            subject.update(ContextSnapshot.class, "reactivate", 5, TimeUnit.MILLISECONDS, error);
        }

        // verify
        assertThat(TELEMETRY.getMetrics())
                .singleElement()
                .satisfies(metric ->
                        assertThat(metric)
                                .hasName("ContextSnapshot.reactivate")
                                .hasDescription("Duration of ContextSnapshot.reactivate")
                                .hasUnit("milliseconds")
                                .hasHistogramSatisfying(histogram -> histogram
                                        .hasPointsSatisfying(point -> point
                                                .hasCount(HUNDRED_THOUSAND)
                                                .hasMin(5.0d)
                                                .hasMax(5.0d)
                                        )));
    }

    @Test
    void update_external_method() {
        // prepare

        // execute
        for (int i = 0; i < HUNDRED_THOUSAND; i++) {
            subject.update(Object.class, "toString", 5, TimeUnit.MILLISECONDS, null);
        }

        // verify
        assertThat(TELEMETRY.getMetrics())
                .singleElement()
                .satisfies(metric ->
                        assertThat(metric)
                                .hasName("java.lang.Object.toString")
                                .hasDescription("Duration of java.lang.Object.toString")
                                .hasUnit("milliseconds")
                                .hasHistogramSatisfying(histogram -> histogram
                                        .hasPointsSatisfying(point -> point
                                                .hasCount(HUNDRED_THOUSAND)
                                                .hasMin(5.0d)
                                                .hasMax(5.0d)
                                        )));
    }
}
