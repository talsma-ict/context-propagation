/*
 * Copyright 2016-2018 Talsma ICT
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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple timer class.
 * <p>
 * To enable rudimentary profiling of context switches:
 * <ol>
 * <li>Set the log level of {@code "nl.talsmasoftware.context.Timing"}
 * to {@code FINEST} level (or {@code TRACE} if using an api like Slf4J)</li>
 * </ol>
 *
 * @author Sjoerd Talsma
 */
final class Timing {
    private static final Logger LOGGER = Logger.getLogger(Timing.class.getName());

    static void timed(long elapsedNanos, Class<?> type, String method) {
        // TODO use actual metrics / tracing api's if available
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "{0}.{1}: {2,number}ns", new Object[]{type.getName(), method, elapsedNanos});
        }
    }

}
