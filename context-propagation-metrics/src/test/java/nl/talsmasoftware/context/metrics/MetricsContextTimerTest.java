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
package nl.talsmasoftware.context.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test class to check whether metrics are actually maintained when creating and reactivating snapshots.
 *
 * @author Sjoerd Talsma
 */
public class MetricsContextTimerTest {

    @Before
    @After
    @SuppressWarnings("unchecked")
    public void resetCacheAndRegistry() {
        try {
            // Reflect and clear the cache
            Field cache = MetricsContextTimer.class.getDeclaredField("TIMERS");
            cache.setAccessible(true);
            ((Map) cache.get(null)).clear();

            // Clear all shared registries
            SharedMetricRegistries.clear();
            Field defaultRegistryName = SharedMetricRegistries.class.getDeclaredField("defaultRegistryName");
            defaultRegistryName.setAccessible(true);
            ((AtomicReference) defaultRegistryName.get(null)).set(null);
        } catch (ReflectiveOperationException roe) {
            throw new AssertionError("Reflection error, possibly the SharedMetricsRegistry changed?", roe);
        }
    }

    @Test
    public void testCreateSnapshotInFreshApplication() {
        assertThat(SharedMetricRegistries.names(), is(empty()));
        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        String name = MetricRegistry.name(ContextManagers.class, "createContextSnapshot");
        assertThat(snapshot, is(notNullValue()));

        assertThat(SharedMetricRegistries.names(), contains("ContextPropagationMetrics"));
        MetricRegistry registry = SharedMetricRegistries.getOrCreate("ContextPropagationMetrics");
        assertThat(registry.getTimers().containsKey(name), is(true));
        assertThat(registry.timer(name).getCount(), is(1L));
    }

    @Test
    public void testCreateSnapshotInApplicationWithDefaultRegistry() {
        SharedMetricRegistries.setDefault("DefaultRegistry");

        assertThat(SharedMetricRegistries.names(), contains("DefaultRegistry"));
        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        String name = MetricRegistry.name(ContextManagers.class, "createContextSnapshot");
        assertThat(snapshot, is(notNullValue()));

        assertThat(SharedMetricRegistries.names(), contains("DefaultRegistry")); // No new registries!
        MetricRegistry registry = SharedMetricRegistries.getDefault();
        assertThat(registry.getTimers().containsKey(name), is(true));
        assertThat(registry.timer(name).getCount(), is(1L));
    }

    @Test
    public void testCreateSnapshotInApplicationWithSingleNonDefaultRegistry() {
        MetricRegistry registry = SharedMetricRegistries.getOrCreate("NonDefaultRegistry");
        assertThat(SharedMetricRegistries.tryGetDefault(), is(nullValue()));

        assertThat(SharedMetricRegistries.names(), contains("NonDefaultRegistry"));
        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        String name = MetricRegistry.name(ContextManagers.class, "createContextSnapshot");
        assertThat(snapshot, is(notNullValue()));

        assertThat(SharedMetricRegistries.names(), contains("NonDefaultRegistry")); // No new registries!
        assertThat(registry.getTimers().containsKey(name), is(true));
        assertThat(registry.timer(name).getCount(), is(1L));
    }

    @Test
    public void testCreate3SnapshotsInApplicationWithMultipleNonDefaultRegistries() {
        MetricRegistry registry1 = SharedMetricRegistries.getOrCreate("NonDefaultRegistry1");
        MetricRegistry registry2 = SharedMetricRegistries.getOrCreate("NonDefaultRegistry2");
        assertThat(SharedMetricRegistries.tryGetDefault(), is(nullValue()));

        assertThat(SharedMetricRegistries.names(), containsInAnyOrder("NonDefaultRegistry1", "NonDefaultRegistry2"));
        for (int i = 1; i <= 3; i++) {
            assertThat(ContextManagers.createContextSnapshot(), is(notNullValue()));
        }
        String name = MetricRegistry.name(ContextManagers.class, "createContextSnapshot");

        assertThat(registry1.getTimers().containsKey(name), is(true));
        assertThat(registry1.timer(name).getCount(), is(3L));

        assertThat(registry2.getTimers().containsKey(name), is(true));
        assertThat(registry2.timer(name).getCount(), is(3L));
    }
}
