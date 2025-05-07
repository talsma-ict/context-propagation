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
package nl.talsmasoftware.context.timers.micrometer;

import io.micrometer.core.instrument.Metrics;
import nl.talsmasoftware.context.api.ContextTimer;

import java.util.concurrent.TimeUnit;

/**
 * {@linkplain ContextTimer} using a {@linkplain io.micrometer.core.instrument.Timer micrometer Timer}
 * to {@linkplain io.micrometer.core.instrument.Timer#record(long, TimeUnit) record} the context switching durations.
 */
public class MicrometerContextTimer implements ContextTimer {

    /**
     * Default constructor.
     *
     * <p>
     * Normally, it is not necessary to instantiate this timer yourself.
     * Providing the {@code context-timer-micrometer} jar file on the classpath
     * should automatically trigger metrics registration using the java ServiceLoader mechanism.
     */
    public MicrometerContextTimer() {
        super(); // Explicit default constructor provided for javadoc.
    }

    /**
     * Updates the {@linkplain io.micrometer.core.instrument.Timer micrometer Timer} specified by the
     * {@code type} and {@code method} with the given {@code duration}.
     *
     * @param type     The class being called
     * @param method   The method being called
     * @param duration The duration of the method
     * @param unit     The unit of the duration
     * @param error    Optional error that occurred when calling the method.
     */
    @Override
    public void update(Class<?> type, String method, long duration, TimeUnit unit, Throwable error) {
        Metrics.timer(type.getName() + "." + method).record(duration, unit);
    }

}
