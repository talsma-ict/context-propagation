/*
 * Copyright 2016-2024 Talsma ICT
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
package nl.talsmasoftware.context.core;

import nl.talsmasoftware.context.api.ContextTimer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Test for the package-protected Timers utility class.
 *
 * @author Sjoerd Talsma
 */
public class TimersTest {

    @Test
    public void testTimingDelegation() {
        Timers.timed(TimeUnit.MILLISECONDS.toNanos(150), getClass(), "testTimingDelegation");
        assertThat(TestContextTimer.getLastTimedMillis(getClass(), "testTimingDelegation"), is(150L));
    }

    public static class TestContextTimer implements ContextTimer {
        private static final Map<String, Long> LAST_TIMED = new HashMap<String, Long>();

        public static Long getLastTimedMillis(Class<?> type, String method) {
            return LAST_TIMED.get(type.getName() + "." + method);
        }

        public void update(Class<?> type, String method, long duration, TimeUnit unit) {
            LAST_TIMED.put(type.getName() + "." + method, unit.toMillis(duration));
        }

    }
}
