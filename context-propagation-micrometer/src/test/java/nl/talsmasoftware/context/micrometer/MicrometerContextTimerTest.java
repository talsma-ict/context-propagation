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
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.number.IsCloseTo.closeTo;

public class MicrometerContextTimerTest {
    SimpleMeterRegistry registry;

    @Before
    public void setupRegistry() {
        registry = new SimpleMeterRegistry();
        Metrics.addRegistry(registry);
    }

    @After
    public void shutdownRegistry() {
        Metrics.removeRegistry(registry);
        registry.clear();
        registry.close();
    }

    @Test
    public void testCreateSnapshotInFreshApplication() {
        Timer timer = Metrics.timer(ContextManagers.class.getName() + ".createContextSnapshot");
        assertThat(timer.count(), is(0L));

        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        assertThat(snapshot, is(notNullValue()));

        assertThat(timer.count(), is(1L));
    }

    @Test
    public void testTiming() {
        Timer timer = Metrics.timer(MicrometerContextTimerTest.class.getName() + ".testTiming");
        new MicrometerContextTimer().update(MicrometerContextTimerTest.class, "testTiming", 43, TimeUnit.MILLISECONDS);
        assertThat(timer.count(), is(1L));
        assertThat(timer.mean(TimeUnit.NANOSECONDS), closeTo(43000000.0d, 0.001d));
    }

}
