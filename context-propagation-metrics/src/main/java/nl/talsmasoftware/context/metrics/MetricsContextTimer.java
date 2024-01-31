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
package nl.talsmasoftware.context.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import nl.talsmasoftware.context.timing.ContextTimer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Collections.singleton;

/**
 * A {@link ContextTimer} that locates an appropriate
 * {@link SharedMetricRegistries shared metric registry}.<br/>
 * It registers a new {@link Timer} for context switches and updates it for each context switch.
 * <p>
 * <dl>
 *     <dt>Shared metric registry resolution</dt>
 *     <dd>
 *         <ol>
 *             <li>First, if a {@code contextpropagation.metrics.registry} system property is defined,
 *             the shared metric registry by that name will be used for all context related timers.</li>
 *             <li>Alternatively, the environment property {@code CONTEXTPROPAGATION_METRICS_REGISTRY} serves the same
 *             purpose if the system property is not defined.</li>
 *             <li>Next, if there is a default shared registry defined
 *             by {@link SharedMetricRegistries#tryGetDefault()}, that one is used.</li>
 *             <li>Otherwise:
 *             <ul>
 *                 <li>If there is exactly one shared registry, that one is used instead of the default.</li>
 *                 <li>If there are no shared registries yet, a new one is created by the name
 *                 {@code "ContextPropagationMetrics"}.</li>
 *                 <li>Otherwise we cannot sensibly choose and try to register new timers
 *                 to <em>all</em> shared registries.</li>
 *             </ul></li>
 *         </ol>
 *     </dd>
 * </dl>
 * <p>
 * Timers are created once and will not be retroactively registered to other shared registries.
 *
 * @author Sjoerd Talsma
 */
public class MetricsContextTimer implements ContextTimer {
    private static final Logger LOGGER = Logger.getLogger(MetricsContextTimer.class.getName());
    private static final String SYS_REGISTRY_NAME = "contextpropagation.metrics.registry";
    private static final String ENV_REGISTRY_NAME = SYS_REGISTRY_NAME.toUpperCase().replace('.', '_');

    private static final ConcurrentMap<String, Timer> TIMERS = new ConcurrentHashMap<>();

    @Override
    public void update(Class<?> type, String method, long duration, TimeUnit unit) {
        locateTimer(type, method).update(duration, unit);
    }

    private static Timer locateTimer(Class<?> type, String method) {
        final String name = MetricRegistry.name(type, method);
        Timer timer = TIMERS.get(name);
        if (timer == null) {
            final Collection<MetricRegistry> sharedRegistries = locateSharedRegistries();
            for (MetricRegistry registry : sharedRegistries) {
                timer = registry.getTimers().get(name);
                if (timer != null) break;
            }
            if (timer == null) timer = new Timer();
            TIMERS.putIfAbsent(name, timer);
            timer = TIMERS.get(name); // In case of race conditions
            for (MetricRegistry registry : sharedRegistries) {
                if (!registry.getTimers().containsKey(name)) registry.register(name, timer);
            }
        }
        return timer;
    }

    private static Collection<MetricRegistry> locateSharedRegistries() {
        String registryName = System.getProperty(SYS_REGISTRY_NAME, System.getenv(ENV_REGISTRY_NAME));
        if (registryName == null) {
            try {
                MetricRegistry registry = SharedMetricRegistries.tryGetDefault();
                if (registry != null) return singleton(registry);
            } catch (NoSuchMethodError libraryTooOld) {
                LOGGER.log(Level.FINE, "Metrics library does not have a SharedMetricRegistries.tryGetDefault method. " +
                        "Please consider updating.", libraryTooOld);
            }
            Set<String> registryNames = SharedMetricRegistries.names();
            if (registryNames.isEmpty()) {
                registryName = "ContextPropagationMetrics";
                LOGGER.log(Level.FINE, "There are no shared metric registries yet, we'll define our own as \"{0}\".", registryName);
            } else if (registryNames.size() == 1) {
                registryName = registryNames.iterator().next();
                LOGGER.log(Level.FINE, "Using single shared registry \"{0}\".", registryName);
            } else {
                List<MetricRegistry> sharedRegistries = new ArrayList<>(registryNames.size());
                for (String name : registryNames) sharedRegistries.add(SharedMetricRegistries.getOrCreate(name));
                LOGGER.log(Level.FINE, "Registering with multiple shared registries: {0}", registryNames);
                return sharedRegistries;
            }
        }
        return singleton(SharedMetricRegistries.getOrCreate(registryName));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{timers=" + TIMERS.keySet() + "}";
    }
}
