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

package nl.talsmasoftware.context;

import javax.imageio.spi.ServiceRegistry;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to allow concurrent systems to {@link #createContextSnapshot() take snapshots of all contexts} from
 * known {@link ContextManager ContextManager} implementations.
 * <p>
 * Such a {@link ContextSnapshot snapshot} can be passed to a background task to allow the context to be
 * {@link ContextSnapshot#reactivate() reactivated} in that background thread, until it gets
 * {@link Context#close() closed} again (preferably in a <code>try-with-resources</code> construct).
 * <p>
 * <center><img src="ContextManagers.svg" alt="Context managers utility-class"></center>
 *
 * @author Sjoerd Talsma
 * @navassoc - creates - ContextSnapshot
 */
public final class ContextManagers {
    private static final Logger LOGGER = Logger.getLogger(ContextManagers.class.getName());

    /**
     * Service locator for registered {@link ContextManager} implementations.
     */
    private static final Loader<ContextManager> LOCATOR = new Loader<ContextManager>(ContextManager.class);

    /**
     * Private constructor to avoid instantiation of this class.
     */
    private ContextManagers() {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is able to create a 'snapshot' from the current
     * {@link ContextManager#getActiveContext() active context} from <em>all known {@link ContextManager}</em>
     * implementations.
     * <p>
     * This snapshot is returned as a single object that can be temporarily
     * {@link ContextSnapshot#reactivate() reactivated}. Don't forget to {@link Context#close() close} the reactivated
     * context once you're done, preferably in a <code>try-with-resources</code> construct.
     * <p>
     * <strong>Note about propagation accross physical node boundaries:</strong>
     * <em>ContextSnapshot instances can be serialized to and reactivated on another node under strict conditions:</em>
     * <ol>
     * <li><strong>All</strong> {@link Context#getValue() context values} <strong>must</strong> be serializable.</li>
     * <li>All {@link ContextManager} implementations that contain
     * {@link ContextManager#getActiveContext() active contexts} must be registered on the target node as well
     * (but need not be serializable themselves).</li>
     * </ol>
     *
     * @return A new snapshot that can be reactivated elsewhere (e.g. a background thread or even another node)
     * within a try-with-resources construct.
     */
    public static ContextSnapshot createContextSnapshot() {
        final Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        boolean noManagers = true;
        for (ContextManager manager : LOCATOR) {
            noManagers = false;
            final Context activeContext = manager.getActiveContext();
            if (activeContext != null) {
                snapshot.put(manager.getClass().getName(), activeContext.getValue());
            }
        }
        if (noManagers) LOGGER.log(Level.WARNING, "Context snapshot was created but no ContextManagers were found!");
        return new ContextSnapshotImpl(snapshot);
    }

    /**
     * Implementation of the <code>createContextSnapshot</code> functionality that can reactivate all values from the
     * snapshot in each corresponding {@link ContextManager}.
     * <p>
     * This class is only really {@link Serializable} if all captured {@link Context#getValue() values} actually are
     * serializable as well. The {@link ContextManager} implementations do not need to be {@link Serializable}.
     */
    private static final class ContextSnapshotImpl implements ContextSnapshot, Serializable {
        private final Map<String, Object> snapshot;

        private ContextSnapshotImpl(Map<String, Object> snapshot) {
            this.snapshot = snapshot;
        }

        /**
         * Tries to get the context manager instance by its classname.<br>
         * This should obviously be available in the {@link #LOCATOR} instance, but if it's not
         * (after deserialization on another malconfigured node?) we will print a warning
         * (due to the potential performance penalty) and return a new instance of the manager.
         *
         * @param contextManagerClassName The class name of the context manager to be returned.
         * @return The appropriate context manager from the locator (hopefully)
         * or a new instance worst-case (warnings will be logged).
         */
        private static ContextManager<?> getContextManagerByName(String contextManagerClassName) {
            for (ContextManager contextManager : LOCATOR) {
                if (contextManagerClassName.equals(contextManager.getClass().getName())) return contextManager;
            }
            LOGGER.log(Level.WARNING, "Context manager \"{0}\" not found in service locator! " +
                    "Attempting to create a new instance as last-resort.", contextManagerClassName);
            try {

                return (ContextManager) Class.forName(contextManagerClassName).getConstructor().newInstance();

            } catch (ClassNotFoundException cnfe) {
                throw new IllegalStateException(String.format(
                        "Context manager \"%s\" is not available on this node!", contextManagerClassName), cnfe);
            } catch (NoSuchMethodException nsme) {
                throw new IllegalStateException(String.format(
                        "Context manager \"%s\" no longer has a default constructor!", contextManagerClassName), nsme);
            } catch (InvocationTargetException ite) {
                throw new IllegalStateException(String.format(
                        "Exception reconstructing ContextManager \"%s\"!", contextManagerClassName), ite.getCause());
            } catch (InstantiationException ie) {
                throw new IllegalStateException(String.format(
                        "Context manager \"%s\" is no longer available!", contextManagerClassName), ie);
            } catch (IllegalAccessException iae) {
                throw new IllegalStateException(String.format(
                        "Not allowed to reload ContextManager \"%s\" on this node!", contextManagerClassName), iae);
            } catch (RuntimeException rte) {
                throw new IllegalStateException(String.format(
                        "Context manager \"%s\" is no longer available!", contextManagerClassName), rte);
            }
        }

        @SuppressWarnings("unchecked") // As we got the values from the managers themselves, they must also accept them!
        public Context<Void> reactivate() {
            final List<Context<?>> reactivatedContexts = new ArrayList<Context<?>>(snapshot.size());
            try {
                for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
                    final ContextManager contextManager = getContextManagerByName(entry.getKey());
                    reactivatedContexts.add(contextManager.initializeNewContext(entry.getValue()));
                }
                return new ReactivatedContext(reactivatedContexts);
            } catch (RuntimeException reactivationException) {
                for (Context alreadyReactivated : reactivatedContexts) {
                    if (alreadyReactivated != null) try { // Undo already reactivated contexts.
                        alreadyReactivated.close();
                    } catch (RuntimeException rte) {
                        addSuppressedOrWarn(reactivationException, rte, "Could not close already reactivated context.");
                    }
                }
                throw reactivationException;
            }
        }

        @Override
        public String toString() {
            return "ContextSnapshot{size=" + snapshot.size() + '}';
        }
    }

    /**
     * Implementation of the reactivated 'container' context that closes all reactivated contexts
     * when it is closed itself.<br>
     * This context contains no meaningful value in itself and purely exists to close the reactivated contexts.
     */
    private static final class ReactivatedContext implements Context<Void> {
        private final List<Context<?>> reactivated;

        private ReactivatedContext(List<Context<?>> reactivated) {
            this.reactivated = reactivated;
        }

        public Void getValue() {
            return null;
        }

        public void close() {
            RuntimeException closeException = null;
            for (Context<?> reactivated : this.reactivated) {
                if (reactivated != null) try {
                    reactivated.close();
                } catch (RuntimeException rte) {
                    if (closeException == null) closeException = rte;
                    else addSuppressedOrWarn(closeException, rte, "Exception closing the reactivated context.");
                }
            }
            if (closeException != null) throw closeException;
        }

        @Override
        public String toString() {
            return "ReactivatedContext{size=" + reactivated.size() + '}';
        }
    }

    private static final Method ADD_SUPPRESSED;

    static {
        Method reflected = null;
        try {
            reflected = Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class);
        } catch (NoSuchMethodException nsme) {
            LOGGER.log(Level.FINEST, "Older JDK detected; the Throwable.addSuppressed() method was not available.", nsme);
        }
        ADD_SUPPRESSED = reflected;
    }

    private static <EX extends Throwable> EX addSuppressedOrWarn(EX exception, Throwable toSuppress, String message) {
        if (ADD_SUPPRESSED != null) try {
            ADD_SUPPRESSED.invoke(exception, toSuppress);
            return exception;
        } catch (InvocationTargetException ite) {
            LOGGER.log(Level.FINEST, "Unexpected exception calling addSuppressed.", ite.getCause());
        } catch (IllegalAccessException iae) {
            LOGGER.log(Level.FINEST, "Not allowed to call addSuppressed: {0}", new Object[]{iae.getMessage(), iae});
        }
        LOGGER.log(Level.WARNING, message, toSuppress);
        return exception;
    }

    /**
     * Loader class to delegate to JDK 6 ServiceLoader or fallback to the old {@link ServiceRegistry}.
     *
     * @param <SVC> The type of service to load.
     */
    private static final class Loader<SVC> implements Iterable<SVC> {
        private final Class<SVC> serviceType;
        private final Iterable<SVC> delegate;

        @SuppressWarnings("unchecked") // Type is actually safe, although we use reflection.
        private Loader(Class<SVC> serviceType) {
            this.serviceType = serviceType;
            Iterable<SVC> serviceLoader = null;
            try { // Attempt to use Java 1.6 ServiceLoader:
                // ServiceLoader.load(ContextManager.class, ContextManagers.class.getClassLoader());
                serviceLoader = (Iterable<SVC>) Class.forName("java.util.ServiceLoader")
                        .getDeclaredMethod("load", Class.class, ClassLoader.class)
                        .invoke(null, serviceType, serviceType.getClassLoader());
            } catch (ClassNotFoundException cnfe) {
                LOGGER.log(Level.FINEST, "Java 6 ServiceLoader not found, falling back to the imageio ServiceRegistry.");
            } catch (NoSuchMethodException nsme) {
                LOGGER.log(Level.SEVERE, "Could not find the 'load' method in the JDK's ServiceLoader.", nsme);
            } catch (IllegalAccessException iae) {
                LOGGER.log(Level.SEVERE, "Not allowed to call the 'load' method in the JDK's ServiceLoader.", iae);
            } catch (InvocationTargetException ite) {
                throw new IllegalStateException(String.format(
                        "Exception calling the 'load' method in the JDK's ServiceLoader for the %s service.",
                        serviceType.getSimpleName()), ite.getCause());
            }
            this.delegate = serviceLoader;
        }

        public Iterator<SVC> iterator() {
            return delegate != null ? delegate.iterator()
                    : ServiceRegistry.lookupProviders(serviceType, serviceType.getClassLoader());
        }
    }
}
