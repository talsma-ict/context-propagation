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
package nl.talsmasoftware.context.dummy;

import nl.talsmasoftware.context.api.ContextTimer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DummyContextTimer implements ContextTimer {
    private static final Map<String, Long> LAST_TIMED = new HashMap<String, Long>();

    public static Long getLastTimedMillis(Class<?> type, String method) {
        return LAST_TIMED.get(type.getName() + "." + method);
    }

    public void update(Class<?> type, String method, long duration, TimeUnit unit, Throwable error) {
        LAST_TIMED.put(type.getName() + "." + method, unit.toMillis(duration));
    }

    public static void clear() {
        LAST_TIMED.clear();
    }
}
