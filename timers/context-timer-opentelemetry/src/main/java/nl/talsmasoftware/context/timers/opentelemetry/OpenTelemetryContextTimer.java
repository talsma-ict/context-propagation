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
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.api.ContextTimer;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

/**
 * {@link nl.talsmasoftware.context.api.ContextTimer Context timer} that
 * creates a {@linkplain io.opentelemetry.api.trace.Span Span}
 * using the {@linkplain io.opentelemetry.api.GlobalOpenTelemetry GlobalOpenTelemetry}
 * for context switches.
 *
 * <p>
 * Individual {@linkplain nl.talsmasoftware.context.api.ContextManager context managers}
 * are <strong>not</strong> traced, only the operations regarding {@linkplain ContextSnapshot}
 * are traced.
 *
 * @author Sjoerd Talsma
 */
public class OpenTelemetryContextTimer implements ContextTimer {
    private static final Logger LOGGER = Logger.getLogger(OpenTelemetryContextTimer.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "nl.talsmasoftware.context";
    private static final String INSTRUMENTATION_VERSION = Optional.ofNullable(readVersion()).orElse("2.0.0");

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
     * Creates a {@linkplain Span} using the {@linkplain GlobalOpenTelemetry} for context switches.
     *
     * <p>
     * Individual {@linkplain nl.talsmasoftware.context.api.ContextManager context managers}
     * are <strong>not</strong> traced, only the operations regarding {@linkplain ContextSnapshot}
     * are traced.
     *
     * @param type     Class that was called.
     * @param method   Method that was called.
     * @param duration Duration of the call.
     * @param unit     Unit of the duration.
     * @param error    Error that was thrown in the call (optional, normally {@code null}).
     */
    @Override
    public void update(Class<?> type, String method, long duration, TimeUnit unit, Throwable error) {
        if (mustTrace(type)) {
            Instant timestamp = Instant.now();
            Span span = GlobalOpenTelemetry.getTracer(INSTRUMENTATION_SCOPE, INSTRUMENTATION_VERSION)
                    .spanBuilder(type.getSimpleName() + "." + method)
                    .setStartTimestamp(timestamp.minusNanos(unit.toNanos(duration)))
                    .setAttribute("context.thread", Thread.currentThread().getName())
                    .startSpan();
            if (error != null) {
                span.recordException(error, Attributes.of(stringKey("exception.message"), error.getMessage()));
            }
            span.end(timestamp);
        }
    }

    private static String readVersion() {
        final String path = "/META-INF/maven/nl.talsmasoftware.context.timers/context-timer-opentelemetry/pom.properties";
        try (InputStream stream = OpenTelemetryContextTimer.class.getResourceAsStream(path)) {
            Properties properties = new Properties();
            properties.load(stream);
            return properties.getProperty("version");
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, e, () -> "Error obtaining version from build metadata (" + path + "): " + e.getMessage());
            return null;
        }
    }

    private static boolean mustTrace(Class<?> type) {
        return ContextSnapshot.class.isAssignableFrom(type);
    }
}
