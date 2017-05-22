/*
 * Copyright 2016-2017 Talsma ICT
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

import javax.imageio.spi.ServiceRegistry;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loader class to delegate to JDK 6 ServiceLoader or fallback to the old {@link ServiceRegistry}.
 *
 * @param <SVC> The type of service to load.
 * @author Sjoerd Talsma
 */
final class Loader<SVC> implements Iterable<SVC> {
    private static final Logger LOGGER = Logger.getLogger(Loader.class.getName());
    private final Class<SVC> serviceType;
    private volatile Iterable<SVC> delegate;

    Loader(Class<SVC> serviceType) {
        this.serviceType = serviceType;
    }

    private synchronized Iterable<SVC> delegate() {
        if (delegate == null) {
            ArrayList<SVC> services = new ArrayList<SVC>();
            Iterator<SVC> iterator = loadServices(serviceType);
            while (iterator.hasNext()) try {
                SVC service = iterator.next();
                if (service != null) services.add(service);
            } catch (NoSuchElementException nse) {
                LOGGER.log(Level.SEVERE, "Exception iterating service " + serviceType + ": " + nse.getMessage(), nse);
            }
            services.trimToSize();
            this.delegate = Collections.unmodifiableList(services);
        }
        return delegate;
    }

    @SuppressWarnings("unchecked") // Type is actually safe, although we use reflection.
    private static <SVC> Iterator<SVC> loadServices(Class<SVC> serviceType) {
        try { // Attempt to use Java 1.6 ServiceLoader:
            // ServiceLoader.load(ContextManager.class, ContextManagers.class.getClassLoader());
            return ((Iterable<SVC>) Class.forName("java.util.ServiceLoader")
                    .getDeclaredMethod("load", Class.class, ClassLoader.class)
                    .invoke(null, serviceType, serviceType.getClassLoader())).iterator();
        } catch (ClassNotFoundException cnfe) {
            LOGGER.log(Level.FINEST, "Java 6 ServiceLoader not found, falling back to the imageio ServiceRegistry.");
        } catch (NoSuchMethodException nsme) {
            LOGGER.log(Level.SEVERE, "Could not find the 'load' method in JDK's ServiceLoader.", nsme);
        } catch (IllegalAccessException iae) {
            LOGGER.log(Level.SEVERE, "Not allowed to call the 'load' method in JDK's ServiceLoader.", iae);
        } catch (InvocationTargetException ite) {
            throw new IllegalStateException(String.format(
                    "Exception calling the 'load' method in JDK's ServiceLoader for the %s service.",
                    serviceType.getSimpleName()), ite.getCause());
        }
        return ServiceRegistry.lookupProviders(serviceType, serviceType.getClassLoader());
    }

    public Iterator<SVC> iterator() {
        return delegate().iterator();
    }
}
