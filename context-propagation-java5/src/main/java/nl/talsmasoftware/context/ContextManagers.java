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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to allow concurrent systems to {@link #createContextSnapshot() take snapshots of all contexts} from
 * known {@link ContextManager ContextManager} implementations.
 * <p>
 * Such a {@link ContextSnapshot snapshot} can be passed to a background task to allow the context to be
 * {@link ContextSnapshot#reactivate() reactivated} in that background thread, until it gets
 * {@link Context#close() closed} again (preferably in a <code>try-with-resources</code> construct).
 *
 * @author Sjoerd Talsma
 */
public final class ContextManagers {
    private static final Logger LOGGER = Logger.getLogger(ContextManagers.class.getName());

    /**
     * Service loader for registered {@link ContextManager} implementations.
     */
    private static final PriorityServiceLoader<ContextManager> SERVICE_LOADER =
            new PriorityServiceLoader<ContextManager>(ContextManager.class);

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
        final long start = System.nanoTime();
        final Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        boolean noManagers = true;
        for (ContextManager manager : SERVICE_LOADER) {
            noManagers = false;
            try {
                long managerStart = System.nanoTime();
                final Context activeContext = manager.getActiveContext();
                if (activeContext == null) {
                    LOGGER.log(Level.FINEST, "There is no active context for {0} in this snapshot.", manager);
                } else {
                    snapshot.put(manager.getClass().getName(), activeContext.getValue());
                    LOGGER.log(Level.FINEST, "Active context of {0} added to new snapshot: {1}.",
                            new Object[]{manager, activeContext});
                    Timing.timed(System.nanoTime() - managerStart, manager.getClass(), "getActiveContext");
                }
            } catch (RuntimeException rte) {
                LOGGER.log(Level.WARNING, "Exception obtaining active context from " + manager + " for snapshot.", rte);
            }
        }
        if (noManagers) {
            SERVICE_LOADER.reload();
            NoContextManagersFound noContextManagersFound = new NoContextManagersFound();
            LOGGER.log(Level.INFO, noContextManagersFound.getMessage(), noContextManagersFound);
        }
        ContextSnapshot result = new ContextSnapshotImpl(snapshot);
        Timing.timed(System.nanoTime() - start, ContextManagers.class, "createContextSnapshot");
        return result;
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
         * This should obviously be available in the {@link #SERVICE_LOADER} instance, but if it's not
         * (after deserialization on another malconfigured node?) we will print a warning
         * (due to the potential performance penalty) and return a new instance of the manager.
         *
         * @param contextManagerClassName The class name of the context manager to be returned.
         * @return The appropriate context manager from the locator (hopefully)
         * or a new instance worst-case (warnings will be logged).
         */
        private static ContextManager<?> getContextManagerByName(String contextManagerClassName) {
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

        public Context<Void> reactivate() {
            final long start = System.nanoTime();
            final Set<String> remainingContextManagers = new LinkedHashSet<String>(snapshot.keySet());
            final List<Context<?>> reactivatedContexts = new ArrayList<Context<?>>(snapshot.size());
            try {
                for (ContextManager contextManager : SERVICE_LOADER) {
                    String contextManagerName = contextManager.getClass().getName();
                    if (remainingContextManagers.remove(contextManagerName)) {
                        reactivatedContexts.add(reactivate(contextManager, snapshot.get(contextManagerName)));
                    }
                }
                for (String contextManagerName : remainingContextManagers) {
                    final ContextManager contextManager = getContextManagerByName(contextManagerName);
                    reactivatedContexts.add(reactivate(contextManager, snapshot.get(contextManagerName)));
                }
                ReactivatedContext reactivatedContext = new ReactivatedContext(reactivatedContexts);
                Timing.timed(System.nanoTime() - start, ContextSnapshot.class, "reactivate");
                return reactivatedContext;
            } catch (RuntimeException reactivationException) {
                for (Context alreadyReactivated : reactivatedContexts) {
                    if (alreadyReactivated != null) try {
                        LOGGER.log(Level.FINEST, "Snapshot reactivation failed! Closing already reactivated context: {0}...", alreadyReactivated);
                        alreadyReactivated.close();
                    } catch (RuntimeException rte) {
                        addSuppressedOrWarn(reactivationException, rte, "Could not close already reactivated context.");
                    }
                }
                throw reactivationException;
            }
        }

        @SuppressWarnings("unchecked") // As we got the values from the managers themselves, they must also accept them!
        private Context reactivate(ContextManager contextManager, Object snapshotValue) {
            long start = System.nanoTime();
            Context reactivated = contextManager.initializeNewContext(snapshotValue);
            LOGGER.log(Level.FINEST, "Context reactivated from snapshot by {0}: {1}.",
                    new Object[]{contextManager, reactivated});
            Timing.timed(System.nanoTime() - start, contextManager.getClass(), "initializeNewContext");
            return reactivated;
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

    @SuppressWarnings("Since15") // That's why we catch the LinkageError here
    private static void addSuppressedOrWarn(Throwable exception, Throwable toSuppress, String message) {
        if (exception != null && toSuppress != null) try {
            exception.addSuppressed(toSuppress);
        } catch (LinkageError le) {
            LOGGER.log(Level.WARNING, message, toSuppress);
        }
    }

    /**
     * Exception that we don't actually throw, but it helps track the issue if we log it including the stacktrace.
     */
    private static class NoContextManagersFound extends RuntimeException {
        private NoContextManagersFound() {
            super("Context snapshot was created but no ContextManagers were found! Current thread: "
                    + Thread.currentThread());
        }
    }
}
