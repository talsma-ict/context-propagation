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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.trace.Span;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.api.ContextTimer;

import java.util.concurrent.TimeUnit;

/**
 * {@link nl.talsmasoftware.context.api.ContextTimer Context timer} that
 * records a duration histogram metric for context switches
 * using the {@linkplain io.opentelemetry.api.GlobalOpenTelemetry GlobalOpenTelemetry}.
 *
 * <p>
 * Individual {@linkplain nl.talsmasoftware.context.api.ContextManager context managers}
 * are <strong>not</strong> metered, only the operations regarding {@linkplain ContextSnapshot}.
 *
 * @author Sjoerd Talsma
 */
public class OpenTelemetryContextTimer implements ContextTimer {
    private static final String INSTRUMENTATION_SCOPE = "nl.talsmasoftware.context";

    /**
     * Default constructor.
     *
     * <p>
     * Normally, it is not necessary to instantiate this timer yourself.
     * Providing the {@code context-timer-opentelemetry} jar file on the classpath
     * should automatically trigger {@linkplain Span} registration using the java ServiceLoader mechanism.
     */
    public OpenTelemetryContextTimer() {
        super(); // Explicit default constructor provided for javadoc.
    }

    /**
     * Records a duration using the {@linkplain GlobalOpenTelemetry} metrics for context switches.
     *
     * <p>
     * Individual {@linkplain nl.talsmasoftware.context.api.ContextManager context managers}
     * are <strong>not</strong> recorded, only the operations regarding {@linkplain ContextSnapshot}
     * are metered.
     *
     * @param type     Class that was called.
     * @param method   Method that was called.
     * @param duration Duration of the call.
     * @param unit     Unit of the duration.
     * @param error    Error that was thrown in the call (optional, normally {@code null}).
     */
    @Override
    public void update(Class<?> type, String method, long duration, TimeUnit unit, Throwable error) {
        getMillisecondTimer(type, method).record(unit.toMillis(duration));
    }

    private static LongHistogram getMillisecondTimer(Class<?> type, String method) {
        final String timerName = getTimerName(type, method);
        return GlobalOpenTelemetry.getMeter(INSTRUMENTATION_SCOPE)
                .histogramBuilder(timerName).ofLongs()
                .setDescription("Duration of " + timerName)
                .setUnit("milliseconds")
                .build();
    }

    private static String getTimerName(Class<?> type, String method) {
        String typeName = type.getName();
        if (typeName.startsWith(INSTRUMENTATION_SCOPE)) {
            typeName = type.getSimpleName();
        }
        return typeName + "." + method;
    }

}
