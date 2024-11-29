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
package nl.talsmasoftware.context.core;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.api.ContextSnapshot;

/**
 * Core implementation to allow {@link #createContextSnapshot() taking a snapshot of all contexts}.
 *
 * <p>
 * Such a {@link ContextSnapshot snapshot} can be passed to a background task to allow the context to be
 * {@link ContextSnapshot#reactivate() reactivated} in that background thread, until it gets
 * {@link Context#close() closed} again (preferably in a <code>try-with-resources</code> construct).
 *
 * @author Sjoerd Talsma
 * @since 1.1.0
 * @deprecated No longer necessary, API provides all functionality from this class now.
 */
@Deprecated
public final class ContextManagers {
//    private static final Logger LOGGER = Logger.getLogger(ContextManagers.class.getName());

    /**
     * Private constructor to avoid instantiation of this class.
     */
    private ContextManagers() {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is able to create a 'snapshot' from the current
     * {@link ContextManager#getActiveContextValue() active context value}
     * from <em>all known {@link ContextManager}</em> implementations.
     *
     * <p>
     * This snapshot is returned as a single object that can be temporarily
     * {@link ContextSnapshot#reactivate() reactivated}. Don't forget to {@link Context#close() close} the reactivated
     * context once you're done, preferably in a <code>try-with-resources</code> construct.
     *
     * @return A new snapshot that can be reactivated elsewhere (e.g. a background thread)
     * within a try-with-resources construct.
     */
    @SuppressWarnings("rawtypes")
    public static ContextSnapshot createContextSnapshot() {
        return ContextSnapshot.capture();
//        final long start = System.nanoTime();
//        final List<ContextManager> managers = ServiceCache.cached(ContextManager.class); // Cached list is immutable
//        final Object[] values = new Object[managers.size()];
//
//        for (int i = 0; i < values.length; i++) {
//            values[i] = getActiveContextValue(managers.get(i));
//        }
//
//        final ContextSnapshotImpl result = new ContextSnapshotImpl(managers, values);
//        if (managers.isEmpty() && LOGGER.isLoggable(Level.FINER)) {
//            LOGGER.finer(result + " was created but no ContextManagers were found! "
//                    + " Thread=" + Thread.currentThread()
//                    + ", ContextClassLoader=" + Thread.currentThread().getContextClassLoader());
//        }
//        Timers.timed(System.nanoTime() - start, ContextManagers.class, "createContextSnapshot", null);
//        return result;
    }

    /**
     * Clears all active contexts from the current thread.
     *
     * <p>
     * Contexts that are 'stacked' (i.e. restore the previous state upon close) should be
     * closed in a way that includes all 'parent' contexts as well.
     *
     * <p>
     * This operation is not intended to be used by general application code as it likely breaks any 'stacked'
     * active context that surrounding code may depend upon.
     * Appropriate use includes thread management, where threads are reused by some pooling
     * mechanism. For example, it is considered safe to clear the context when obtaining a 'fresh' thread from a
     * thread pool (as no context expectations should exist at that point).
     * An even better strategy would be to clear the context right before returning a used thread to the pool
     * as this will allow any unclosed contexts to be garbage collected. Besides preventing contextual issues,
     * this reduces the risk of memory leaks by unbalanced context calls.
     */
    public static void clearActiveContexts() {
        ContextManager.clearAll();
//        final long start = System.nanoTime();
//        for (ContextManager<?> manager : ServiceCache.cached(ContextManager.class)) {
//            clear(manager);
//        }
//        Timers.timed(System.nanoTime() - start, ContextManagers.class, "clearActiveContexts", null);
    }

    /**
     * Override the {@linkplain ClassLoader} used to lookup {@linkplain ContextManager contextmanagers}.
     * <p>
     * Normally, taking a snapshot uses the {@linkplain Thread#getContextClassLoader() Context ClassLoader} from the
     * {@linkplain Thread#currentThread() current thread} to look up all {@linkplain ContextManager context managers}.
     * It is possible to configure a fixed, single classloader in your application for looking up the context managers.
     * <p>
     * Using this method to specify a fixed classloader will only impact
     * new {@linkplain ContextSnapshot context snapshots}. Existing snapshots will not be impacted.
     * <p>
     * <strong>Notes:</strong><br>
     * <ul>
     * <li>Please be aware that this configuration is global!
     * <li>This will also affect the lookup of
     * {@linkplain nl.talsmasoftware.context.api.ContextTimer context timers}
     * </ul>
     *
     * @param classLoader The single, fixed ClassLoader to use for finding context managers.
     *                    Specify {@code null} to restore the default behaviour.
     * @since 1.0.5
     */
    public static synchronized void useClassLoader(ClassLoader classLoader) {
//        ServiceCache.useClassLoader(classLoader);
        ContextManager.useClassLoader(classLoader);
    }

//    private static Object getActiveContextValue(ContextManager<?> manager) {
//        final long start = System.nanoTime();
//        RuntimeException error = null;
//        try {
//
//            final Object activeContextValue = manager.getActiveContextValue();
//            LOGGER.finest(() -> activeContextValue == null
//                    ? "There is no active context value for " + manager + " (in thread " + Thread.currentThread().getName() + ")."
//                    : "Active context value of " + manager + " in " + Thread.currentThread().getName() + ": " + activeContextValue);
//            return activeContextValue;
//
//        } catch (RuntimeException e) {
//            LOGGER.log(Level.WARNING, e, () -> "Error obtaining active context from " + manager + " (in thread " + Thread.currentThread().getName() + ").");
//            error = e;
//            return null;
//        } finally {
//            Timers.timed(System.nanoTime() - start, manager.getClass(), "getActiveContextValue", error);
//        }
//    }

//    private static void clear(ContextManager<?> manager) {
//        final long start = System.nanoTime();
//        RuntimeException error = null;
//        try {
//
//            manager.clear();
//            LOGGER.finest(() -> "Active context of " + manager + " was cleared.");
//
//        } catch (RuntimeException e) {
//            LOGGER.log(Level.WARNING, e, () -> "Error clearing active context from " + manager + "(in thread " + Thread.currentThread().getName() + ").");
//            error = e;
//        } finally {
//            Timers.timed(System.nanoTime() - start, manager.getClass(), "clear", error);
//        }
//    }

//    /**
//     * Implementation of the <code>createContextSnapshot</code> functionality that can reactivate all values from the
//     * snapshot in each corresponding {@link ContextManager}.
//     */
//    @SuppressWarnings("rawtypes")
//    private static final class ContextSnapshotImpl implements ContextSnapshot {
//        private final List<ContextManager> managers;
//        private final Object[] values;
//
//        private ContextSnapshotImpl(List<ContextManager> managers, Object[] values) {
//            this.managers = managers;
//            this.values = values;
//        }
//
//        public Reactivation reactivate() {
//            final long start = System.nanoTime();
//            RuntimeException error = null;
//            final Context[] reactivatedContexts = new Context[managers.size()];
//            try {
//
//                for (int i = 0; i < values.length; i++) {
//                    reactivatedContexts[i] = reactivate(managers.get(i), values[i]);
//                }
//                return new ReactivationImpl(reactivatedContexts);
//
//            } catch (RuntimeException reactivationException) {
//                tryClose(reactivatedContexts, reactivationException);
//                ServiceCache.clear();
//                throw error = reactivationException;
//            } finally {
//                Timers.timed(System.nanoTime() - start, ContextSnapshot.class, "reactivate", error);
//            }
//        }
//
//        @Override
//        public String toString() {
//            return "ContextSnapshot{size=" + managers.size() + '}';
//        }
//
//        /**
//         * Reactivates a snapshot value for a single context manager.
//         *
//         * <p>
//         * This initializes a new context with the context manager
//         * (normally on another thread the snapshot value was captured from).
//         *
//         * @param contextManager The context manager to reactivate the snapshot value for.
//         * @param snapshotValue  The snapshot value to be reactivated.
//         * @return The context to be included in the reactivation object.
//         */
//        @SuppressWarnings("unchecked") // We got the value from the managers itself.
//        private static Context reactivate(ContextManager contextManager, Object snapshotValue) {
//            if (snapshotValue == null) return null;
//            long start = System.nanoTime();
//            RuntimeException error = null;
//            try {
//
//                Context reactivated = contextManager.initializeNewContext(snapshotValue);
//                LOGGER.finest(() -> "Context reactivated from snapshot by " + contextManager + ": " + reactivated + ".");
//                return reactivated;
//
//            } catch (RuntimeException e) {
//                throw error = e;
//            } finally {
//                Timers.timed(System.nanoTime() - start, contextManager.getClass(), "initializeNewContext", error);
//            }
//        }
//
//        /**
//         * Try to close already-reactivated contexts when a later context manager threw an exception.
//         *
//         * @param reactivatedContexts The contexts that were already reactivated when the error happened.
//         * @param reason              The error that happened.
//         */
//        private static void tryClose(Context[] reactivatedContexts, Throwable reason) {
//            for (Context alreadyReactivated : reactivatedContexts) {
//                if (alreadyReactivated != null) {
//                    try {
//                        alreadyReactivated.close();
//                    } catch (RuntimeException rte) {
//                        reason.addSuppressed(rte);
//                    }
//                }
//            }
//        }
//    }

//    /**
//     * Implementation of the reactivated 'container' context that closes all reactivated contexts
//     * when it is closed itself.<br>
//     * This context contains no meaningful value in itself and purely exists to close the reactivated contexts.
//     */
//    @SuppressWarnings("rawtypes")
//    private static final class ReactivationImpl implements Reactivation {
//        private final Context[] reactivated;
//
//        private ReactivationImpl(Context[] reactivated) {
//            this.reactivated = reactivated;
//        }
//
//        public void close() {
//            RuntimeException closeException = null;
//            // close in reverse order of reactivation
//            for (int i = this.reactivated.length - 1; i >= 0; i--) {
//                Context<?> reactivated = this.reactivated[i];
//                if (reactivated != null) try {
//                    reactivated.close();
//                } catch (RuntimeException rte) {
//                    if (closeException == null) closeException = rte;
//                    else closeException.addSuppressed(rte);
//                }
//            }
//            if (closeException != null) throw closeException;
//        }
//
//        @Override
//        public String toString() {
//            return "ContextSnapshot.Reactivation{size=" + reactivated.length + '}';
//        }
//    }
}
