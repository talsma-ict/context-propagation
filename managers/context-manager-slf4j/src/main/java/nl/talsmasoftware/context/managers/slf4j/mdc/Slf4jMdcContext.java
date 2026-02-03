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
package nl.talsmasoftware.context.managers.slf4j.mdc;

import nl.talsmasoftware.context.api.Context;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

final class Slf4jMdcContext implements Context {
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Map<String, String> previous;

    Slf4jMdcContext(Map<String, String> values) {
        // Capture current MDC as 'previous' and make the given values the 'new current' MDC.
        this.previous = new HashMap<>(values.size());
        applyMdcValues(values, previous::put);
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            applyMdcValues(previous, null);
        }
    }

    /**
     * Apply specified MDC values.
     *
     * @param mdcValues              The MDC values to make active.
     * @param previousValuesConsumer Consumer for previous values if they are important (optional, can be null).
     */
    private static void applyMdcValues(Map<String, String> mdcValues, BiConsumer<String, String> previousValuesConsumer) {
        mdcValues.forEach((key, value) -> {
            if (mustPropagate(key)) {
                keepPreviousValue(previousValuesConsumer, key);
                applyMdcValue(key, value);
            }
        });
    }

    private static boolean mustPropagate(String mdcKey) {
        // Built-in for now, possibly through a MdcKeyFilter SPI mechanism later is someone asks for it.
        return !mdcKey.toLowerCase(Locale.ROOT).contains("thread"); // Don't propagate thread-specific values by default
    }

    private static void keepPreviousValue(BiConsumer<String, String> previousValuesConsumer, String key) {
        if (previousValuesConsumer != null) {
            previousValuesConsumer.accept(key, MDC.get(key));
        }
    }

    private static void applyMdcValue(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
