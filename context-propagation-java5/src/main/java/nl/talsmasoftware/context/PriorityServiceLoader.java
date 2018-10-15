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
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
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

    private final Class<SVC> serviceType;

    PriorityServiceLoader(Class<SVC> serviceType) {
        if (serviceType == null) throw new NullPointerException("Service type is <null>.");
        this.serviceType = serviceType;
    }

    @SuppressWarnings("unchecked")
    public synchronized Iterator<SVC> iterator() {
        ArrayList<SVC> services = new ArrayList<SVC>();
        for (Iterator<SVC> iterator = loadServices(serviceType); iterator.hasNext(); ) {
            SVC service = iterator.next();
            if (service != null) services.add(service);
        }

        if (services.isEmpty()) {
            return (Iterator<SVC>) emptySet().iterator();
        } else if (services.size() == 1) {
            return singleton(services.get(0)).iterator();
        }
        if (PriorityComparator.PRIORITY_AVAILABLE) sort(services, PriorityComparator.INSTANCE);
        return unmodifiableList(services).iterator();
    }

    @SuppressWarnings("Since15")
    private static <SVC> Iterator<SVC> loadServices(Class<SVC> serviceType) {
        try {
            return java.util.ServiceLoader.load(serviceType).iterator();
        } catch (LinkageError le) {
            LOGGER.log(Level.FINEST, "No ServiceLoader available, probably running on Java 1.5.", le);
            return javax.imageio.spi.ServiceRegistry.lookupProviders(serviceType);
        } catch (RuntimeException loadingException) {
            LOGGER.log(Level.WARNING, "Unexpected error loading services of " + serviceType, loadingException);
            return Collections.<SVC>emptySet().iterator();
        }
    }

}
