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
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import nl.talsmasoftware.context.api.ContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class MicrometerContextTimerTest {
    SimpleMeterRegistry registry;

    @BeforeEach
    void setupRegistry() {
        registry = new SimpleMeterRegistry();
        Metrics.addRegistry(registry);
    }

    @AfterEach
    void shutdownRegistry() {
        Metrics.removeRegistry(registry);
        registry.clear();
        registry.close();
    }

    @Test
    void testCreateSnapshotInFreshApplication() {
        Timer timer = Metrics.timer(ContextSnapshot.class.getName() + ".capture");
        assertThat(timer.count()).isZero();

        ContextSnapshot snapshot = ContextSnapshot.capture();
        assertThat(snapshot).isNotNull();

        assertThat(timer.count()).isEqualTo(1L);
    }

    @Test
    void testTiming() {
        Timer timer = Metrics.timer(MicrometerContextTimerTest.class.getName() + ".testTiming");
        new MicrometerContextTimer().update(MicrometerContextTimerTest.class, "testTiming", 43, TimeUnit.MILLISECONDS, null);
        assertThat(timer.count()).isEqualTo(1L);
        assertThat(timer.mean(TimeUnit.NANOSECONDS)).isCloseTo(43000000.0d, offset(0.001d));
    }

}
