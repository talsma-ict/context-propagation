/*
 * Copyright (C) 2016 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.talsmasoftware.concurrency.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Sjoerd Talsma
 */
public final class ContextManagers {

    private static AtomicReference<Collection<ContextManager<?>>> instances =
            new AtomicReference<Collection<ContextManager<?>>>();

    /**
     * Private constructor to avoid instantiation of this class.
     */
    private ContextManagers() {
        throw new UnsupportedOperationException();
    }

    private static Collection<ContextManager<?>> contextManagers() {
        if (instances.get() == null) {
            final String servicesResourceName = "META-INF/services/" + ContextManager.class.getName();
            try {
                final List<RuntimeException> errors = new ArrayList<RuntimeException>();
                final ClassLoader classLoader = ContextManagers.class.getClassLoader();
                for (Enumeration<URL> en = classLoader.getResources(servicesResourceName); en.hasMoreElements(); ) {
                    final URL url = en.nextElement();
                    final InputStream in = url.openStream();
                    String line = null;
                    try {
                        final BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                        for (line = reader.readLine(); line != null; line = reader.readLine()) {
                            Class<?> aClass = classLoader.loadClass(line.trim());
// TODO: We don't want to instantiate the context managers here, or do we??
                        }
                    } catch (ClassNotFoundException cnfe) {
                        throw new IllegalStateException("Error loading class \"" + line.trim() + "\"!", cnfe);
                    } finally {
                        if (in != null) try {
                            in.close();
                        } catch (IOException ioe) {
                            throw new IllegalStateException("Error loading resource \"" + url + "\": " + ioe.getMessage(), ioe);
                        }
                    }
                }
                final int errorCount = errors.size();
                if (errorCount == 1) throw errors.get(0);
            } catch (IOException ioe) {
                throw new IllegalStateException("Unable to load \"" + servicesResourceName + "\"!", ioe);
            } catch (RuntimeException rte) {
                throw new IllegalStateException("Unable to load \"" + servicesResourceName + "\"!", rte);
            }
        }
        return instances.get();
    }


//    private static List<Class<?>> discoverServices(Class<?> klass) {
//        final List<Class<?>> serviceClasses = new ArrayList<>();
//        try {
//            // use classloader that loaded this class to find the service descriptors on the classpath
//            // better than ClassLoader.getSystemResources() which may not be the same classloader if ths app
//            // is running in a container (e.g. via maven exec:java)
//            final Enumeration<URL> resources = getClassLoader().getResources("META-INF/services/" + klass.getName());
//            while (resources.hasMoreElements()) {
//                final URL url = resources.nextElement();
//                try (InputStream input = url.openStream();
//                     InputStreamReader streamReader = new InputStreamReader(input, StandardCharsets.UTF_8);
//                     BufferedReader reader = new BufferedReader(streamReader)) {
//                    String line;
//                    while ((line = reader.readLine()) != null) {
//                        final Class<?> loadedClass = loadClass(line);
//                        if (loadedClass != null) {
//                            serviceClasses.add(loadedClass);
//                        }
//                    }
//                }
//            }
//        } catch (IOException e) {
//            LOGGER.warn("Unable to load META-INF/services/{}", klass.getName(), e);
//        }
//        return serviceClasses;
//    }

}
