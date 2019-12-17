/*
 * Copyright 2016-2019 Talsma ICT
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
package nl.talsmasoftware.context.micrometer;

import io.micrometer.core.instrument.Metrics;
import nl.talsmasoftware.context.timing.ContextTimer;

import java.util.concurrent.TimeUnit;

/**
 * {@linkplain ContextTimer} using a {@linkplain io.micrometer.core.instrument.Timer micrometer Timer}
 * to {@linkplain io.micrometer.core.instrument.Timer#record(long, TimeUnit) record} the context switching durations.
 */
public class MicrometerContextTimer implements ContextTimer {

    /**
     * Updates the {@linkplain io.micrometer.core.instrument.Timer micrometer Timer} specified by the
     * {@code type} and {@code method} with the given {@code duration}.
     *
     * @param type     The class being called
     * @param method   The method being called
     * @param duration The duration of the method
     * @param unit     The unit of the duration
     */
    @Override
    public void update(Class<?> type, String method, long duration, TimeUnit unit) {
        Metrics.timer(type.getName() + "." + method).record(duration, unit);
    }

}
