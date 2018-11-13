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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableList;

/**
 * Loader class to delegate to JDK 6 {@code ServiceLoader} or fallback to the old
 * {@link javax.imageio.spi.ServiceRegistry ServiceRegistry}.
 *
 * @param <SVC> The type of service to load.
 * @author Sjoerd Talsma
 */
final class PriorityServiceLoader<SVC> implements Iterable<SVC> {
    private static final Logger LOGGER = Logger.getLogger(PriorityServiceLoader.class.getName());
    private static final String SYSTEMPROPERTY_CACHING = "talsmasoftware.context.caching";

    private final Class<SVC> serviceType;
    private final boolean cachingDisabled;
    private final Map<ClassLoader, List<SVC>> cache = new WeakHashMap<ClassLoader, List<SVC>>();

    PriorityServiceLoader(Class<SVC> serviceType) {
        if (serviceType == null) throw new NullPointerException("Service type is <null>.");
        this.serviceType = serviceType;
        final String cachingProperty = System.getProperty(SYSTEMPROPERTY_CACHING,
                System.getenv(SYSTEMPROPERTY_CACHING.replace('.', '_').toUpperCase()));
        this.cachingDisabled = "0".equals(cachingProperty) || "false".equalsIgnoreCase(cachingProperty);
    }

    @SuppressWarnings("unchecked")
    public Iterator<SVC> iterator() {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        List<SVC> services = cache.get(cl);
        if (services == null) {
            services = new ArrayList<SVC>();
            for (Iterator<SVC> iterator = loadServices(serviceType, cl); iterator.hasNext(); ) {
                SVC service = iterator.next();
                if (service != null) services.add(service);
            }
            services = sortAndMakeUnmodifiable(services);
            if (!cachingDisabled) cache.put(cl, services);
        }
        return services.iterator();
    }

    /**
     * Removes the cache to the next call to {@linkplain #iterator()} will attempt to load the objects again.
     */
    void clearCache() {
        cache.clear();
    }

    private static <T> List<T> sortAndMakeUnmodifiable(List<T> services) {
        if (services.isEmpty()) return emptyList();
        else if (services.size() == 1) return singletonList(services.get(0));
        else if (PriorityComparator.PRIORITY_AVAILABLE) sort(services, PriorityComparator.INSTANCE);
        return unmodifiableList(services);
    }

    @SuppressWarnings("Since15")
    private static <SVC> Iterator<SVC> loadServices(Class<SVC> serviceType, ClassLoader classLoader) {
        try {
            return java.util.ServiceLoader.load(serviceType, classLoader).iterator();
        } catch (LinkageError le) {
            LOGGER.log(Level.FINEST, "No ServiceLoader available, probably running on Java 1.5.", le);
            return javax.imageio.spi.ServiceRegistry.lookupProviders(serviceType, classLoader);
        } catch (RuntimeException loadingException) {
            LOGGER.log(Level.WARNING, "Unexpected error loading services of " + serviceType, loadingException);
            return Collections.<SVC>emptySet().iterator();
        }
    }

}
