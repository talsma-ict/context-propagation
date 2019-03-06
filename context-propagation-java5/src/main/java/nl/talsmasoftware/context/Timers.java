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
package nl.talsmasoftware.context;

import nl.talsmasoftware.context.timing.ContextTimer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton initialized to lookup all {@link ContextTimer} delegates.
 *
 * @author Sjoerd Talsma
 */
final class Timers {
    private static final Logger TIMING_LOGGER = Logger.getLogger("nl.talsmasoftware.context.Timing");

    /**
     * Singleton containing resolved ContextTimer delegates.
     */
    private enum Singleton {
        INSTANCE;
        private final ContextTimer[] delegates;

        Singleton() {
            List<ContextTimer> delegates = new ArrayList<ContextTimer>();
            for (ContextTimer delegate : new PriorityServiceLoader<ContextTimer>(ContextTimer.class)) {
                delegates.add(delegate);
            }
            this.delegates = delegates.toArray(new ContextTimer[0]);
        }
    }

    static void timed(long durationNanos, Class<?> type, String method) {
        for (ContextTimer delegate : Singleton.INSTANCE.delegates) {
            delegate.update(type, method, durationNanos, TimeUnit.NANOSECONDS);
        }
        if (TIMING_LOGGER.isLoggable(Level.FINEST)) {
            TIMING_LOGGER.log(Level.FINEST, "{0}.{1}: {2,number}ns", new Object[]{type.getName(), method, durationNanos});
        }
    }

}
