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

import nl.talsmasoftware.context.clearable.Clearable;

import java.io.Serializable;
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
     * The service loader that loads (and possibly caches) {@linkplain ContextManager} instances in prioritized order.
     */
    private static final PriorityServiceLoader<ContextManager> CONTEXT_MANAGERS =
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
        Long managerStart = null;
        for (ContextManager manager : CONTEXT_MANAGERS) {
            managerStart = System.nanoTime();
            try {
                final Context activeContext = manager.getActiveContext();
                if (activeContext != null) {
                    snapshot.put(manager.getClass().getName(), activeContext.getValue());
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Active context of " + manager + " added to new snapshot: " + activeContext + ".");
                    }
                    Timing.timed(System.nanoTime() - managerStart, manager.getClass(), "getActiveContext");
                } else if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "There is no active context for " + manager + " in this snapshot.");
                }
            } catch (RuntimeException rte) {
                CONTEXT_MANAGERS.clearCache();
                LOGGER.log(Level.WARNING, "Exception obtaining active context from " + manager + " for snapshot.", rte);
                Timing.timed(System.nanoTime() - managerStart, manager.getClass(), "getActiveContext.exception");
            }
        }
        if (managerStart == null) {
            NoContextManagersFound noContextManagersFound = new NoContextManagersFound();
            LOGGER.log(Level.INFO, noContextManagersFound.getMessage(), noContextManagersFound);
        }
        ContextSnapshot result = new ContextSnapshotImpl(snapshot);
        Timing.timed(System.nanoTime() - start, ContextManagers.class, "createContextSnapshot");
        return result;
    }

    /**
     * Clears all active contexts from the current thread.
     * <p>
     * Contexts that are 'stacked' (i.e. restore the previous state upon close) should be
     * closed in a way that includes all 'parent' contexts as well.
     * <p>
     * This operation is not intended to be used by general application code as it likely breaks any 'stacked'
     * active context that surrounding code may depend upon.
     * Appropriate use includes thread management, where threads are reused by some pooling
     * mechanism. For example, it is considered safe to clear the context when obtaining a 'fresh' thread from a
     * thread pool (as no context expectations should exist at that point).
     * An even better strategy would be to clear the context right before returning a used thread to the pool
     * as this will allow any unclosed contexts to be garbage collected. Besides preventing contextual issues,
     * this reduces the risk of memory leaks by unbalanced context calls.
     * <p>
     * For context managers that are not {@linkplain Clearable} and contain an active {@linkplain Context}
     * that is not {@code Clearable} either, this active context will be closed normally.
     */
    public static void clearActiveContexts() {
        final long start = System.nanoTime();
        Long managerStart = null;
        for (ContextManager manager : CONTEXT_MANAGERS) {
            managerStart = System.nanoTime();
            try {
                if (manager instanceof Clearable) {
                    ((Clearable) manager).clear();
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Active context of " + manager + " was cleared.");
                    }
                    Timing.timed(System.nanoTime() - managerStart, manager.getClass(), "clear");
                } else {
                    Context activeContext = manager.getActiveContext();
                    if (activeContext != null) {
                        clearContext(manager, activeContext);
                    } else if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("There is no active context for " + manager + " to be cleared.");
                    }
                }
            } catch (RuntimeException rte) {
                LOGGER.log(Level.WARNING, "Exception clearing active context from " + manager + ".", rte);
                Timing.timed(System.nanoTime() - managerStart, manager.getClass(), "clear.exception");
            }
        }
        if (managerStart == null) {
            NoContextManagersFound noContextManagersFound = new NoContextManagersFound();
            LOGGER.log(Level.INFO, noContextManagersFound.getMessage(), noContextManagersFound);
        }
        Timing.timed(System.nanoTime() - start, ContextManagers.class, "clearActiveContexts");
    }

    private static void clearContext(ContextManager manager, Context context) {
        final long start = System.nanoTime();
        final Class<? extends Context> contextType = context.getClass();
        if (context instanceof Clearable) {
            ((Clearable) context).clear();
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("Active context of " + manager + " was cleared.");
            }
        } else {
            int maxAttempts = 255;
            while (context != null && --maxAttempts > 0) {
                context.close();
                context = manager.getActiveContext();
            }
            if (context != null) {
                Logger.getLogger(manager.getClass().getName()).warning(
                        "Possible endless loop prevented clearing the active context for " + manager +
                                ". Could it be that this manager returns a non-null context by default " +
                                "and has not implemented the Clearable interface?");
            }
        }
        Timing.timed(System.nanoTime() - start, contextType, "clear");
    }

    /**
     * Implementation of the <code>createContextSnapshot</code> functionality that can reactivate all values from the
     * snapshot in each corresponding {@link ContextManager}.
     * <p>
     * This class is only really {@link Serializable} if all captured {@link Context#getValue() values} are
     * serializable as well. The {@link ContextManager} implementations do not need to be {@link Serializable}.
     */
    private static final class ContextSnapshotImpl implements ContextSnapshot, Serializable {
        private final Map<String, Object> snapshot;

        private ContextSnapshotImpl(Map<String, Object> snapshot) {
            this.snapshot = snapshot;
        }

        public Context<Void> reactivate() {
            final long start = System.nanoTime();
            final Set<String> remainingContextManagers = new LinkedHashSet<String>(snapshot.keySet());
            final List<Context<?>> reactivatedContexts = new ArrayList<Context<?>>(snapshot.size());
            try {
                for (ContextManager contextManager : CONTEXT_MANAGERS) {
                    String contextManagerName = contextManager.getClass().getName();
                    if (remainingContextManagers.remove(contextManagerName)) {
                        reactivatedContexts.add(reactivate(contextManager, snapshot.get(contextManagerName)));
                    }
                }

                if (!remainingContextManagers.isEmpty()) { // Should not happen, print warnings!
                    CONTEXT_MANAGERS.clearCache();
                    for (String contextManagerName : remainingContextManagers) {
                        LOGGER.log(Level.WARNING, "Context manager \"{0}\" not found in service loader! " +
                                "Cannot reactivate: {1}", new Object[]{contextManagerName, snapshot.get(contextManagerName)});
                    }
                }
                ReactivatedContext reactivatedContext = new ReactivatedContext(reactivatedContexts);
                Timing.timed(System.nanoTime() - start, ContextSnapshot.class, "reactivate");
                return reactivatedContext;
            } catch (RuntimeException reactivationException) {
                CONTEXT_MANAGERS.clearCache();
                for (Context alreadyReactivated : reactivatedContexts) {
                    if (alreadyReactivated != null) try {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Snapshot reactivation failed! " +
                                    "Closing already reactivated context: " + alreadyReactivated + ".");
                        }
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
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("Context reactivated from snapshot by " + contextManager + ": " + reactivated + ".");
            }
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
