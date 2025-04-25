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
package nl.talsmasoftware.context.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Cache for resolved services.
 *
 * <p>
 * This is necessary because the ServiceLoader itself is not thread-safe due to its internal lazy iterator.
 *
 * <p>
 * Only intended for interal use.
 */
final class ServiceCache {
    private static final Logger LOGGER = Logger.getLogger(ServiceCache.class.getName());

    /**
     * Internal concurrent map as cache.
     */
    @SuppressWarnings("rawtypes")
    private static final ConcurrentMap<Class, List> CACHE = new ConcurrentHashMap<>();

    /**
     * Sometimes a single, fixed classloader may be necessary (e.g. #97)
     */
    @SuppressWarnings("java:S3077") // The classloader is out of our control, the volatile reference is what we need.
    private static volatile ClassLoader classLoaderOverride = null;

    private ServiceCache() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    static synchronized void useClassLoader(ClassLoader classLoader) {
        if (classLoaderOverride == classLoader) {
            LOGGER.finest(() -> "Maintaining classloader override as " + classLoader + " (unchanged)");
            return;
        }
        LOGGER.fine(() -> "Updating classloader override to " + classLoader + " (was: " + classLoaderOverride + ")");
        classLoaderOverride = classLoader;
        CACHE.clear();
    }

    @SuppressWarnings("unchecked")
    static <T> List<T> cached(Class<T> serviceClass) {
        return CACHE.computeIfAbsent(serviceClass, ServiceCache::load);
    }

    static void clear() {
        CACHE.clear();
    }

    /**
     * Loads the service implementations of the requested type.
     *
     * <p>
     * This method is synchronized because ServiceLoader is not thread-safe.
     * Fortunately this only gets called after a cache miss, so should not affect performance.
     *
     * <p>
     * The returned {@code List} will be {@linkplain Collections#unmodifiableList(List) unmodifiable}.
     *
     * @param serviceType The service type to load.
     * @param <T>         The service type to load.
     * @return Unmodifiable list of service implementations.
     */
    private static synchronized <T> List<T> load(Class<T> serviceType) {
        final ArrayList<T> services = new ArrayList<>();
        final ServiceLoader<T> loader = classLoaderOverride == null
                ? ServiceLoader.load(serviceType)
                : ServiceLoader.load(serviceType, classLoaderOverride);
        for (T service : loader) {
            services.add(service);
        }
        services.trimToSize();
        LOGGER.fine(() -> String.format("Loaded %d %s service implementations: %s.", services.size(), serviceType.getSimpleName(), services));
        return Collections.unmodifiableList(services);
    }
}
