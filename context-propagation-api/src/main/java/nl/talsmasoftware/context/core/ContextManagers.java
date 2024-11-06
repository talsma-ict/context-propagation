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
import nl.talsmasoftware.context.api.ContextSnapshot.Reactivation;
import nl.talsmasoftware.context.api.ContextTimer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 */
public final class ContextManagers {
    private static final Logger LOGGER = Logger.getLogger(ContextManagers.class.getName());

    /**
     * Sometimes a single, fixed classloader may be necessary (e.g. #97)
     */
    private static volatile ClassLoader classLoaderOverride = null;

    private static volatile List<ContextManager<?>> contextManagers = null;

    private static volatile List<ContextTimer> contextTimers = null;

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
    public static nl.talsmasoftware.context.api.ContextSnapshot createContextSnapshot() {
        final long start = System.nanoTime();
        final List<ContextManager<?>> managers = new LinkedList<>();
        final List<Object> values = new LinkedList<>();
        Long managerStart = null;
        for (ContextManager<?> manager : getContextManagers()) {
            managerStart = System.nanoTime();
            try {
                final Object activeContextValue = manager.getActiveContextValue();
                if (activeContextValue != null) {
                    values.add(activeContextValue);
                    managers.add(manager);
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Active context value of " + manager + " added to new snapshot: " + activeContextValue);
                    }
                    Timers.timed(System.nanoTime() - managerStart, manager.getClass(), "getActiveContext");
                } else if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "There is no active context for " + manager + " in this snapshot.");
                }
            } catch (RuntimeException rte) {
                LOGGER.log(Level.WARNING, "Exception obtaining active context from " + manager + " for snapshot.", rte);
                Timers.timed(System.nanoTime() - managerStart, manager.getClass(), "getActiveContext.exception");
            }
        }
        if (managerStart == null) {
            NoContextManagersFound noContextManagersFound = new NoContextManagersFound();
            LOGGER.log(Level.INFO, noContextManagersFound.getMessage(), noContextManagersFound);
        }
        ContextSnapshotImpl result = new ContextSnapshotImpl(managers, values);
        Timers.timed(System.nanoTime() - start, ContextManagers.class, "createContextSnapshot");
        return result;
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
        final long start = System.nanoTime();
        Long managerStart = null;
        for (ContextManager<?> manager : getContextManagers()) {
            managerStart = System.nanoTime();
            try {
                manager.clear();
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.finest("Active context of " + manager + " was cleared.");
                }
                Timers.timed(System.nanoTime() - managerStart, manager.getClass(), "clear");
            } catch (RuntimeException rte) {
                LOGGER.log(Level.WARNING, "Exception clearing active context from " + manager + ".", rte);
                contextManagers = null;
                Timers.timed(System.nanoTime() - managerStart, manager.getClass(), "clear.exception");
            }
        }
        if (managerStart == null) {
            NoContextManagersFound noContextManagersFound = new NoContextManagersFound();
            LOGGER.log(Level.INFO, noContextManagersFound.getMessage(), noContextManagersFound);
        }
        Timers.timed(System.nanoTime() - start, ContextManagers.class, "clearActiveContexts");
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
        if (classLoaderOverride == classLoader) {
            LOGGER.finest(() -> "Maintaining classloader override as " + classLoader + " (unchanged)");
            return;
        }
        LOGGER.fine(() -> "Updating classloader override to " + classLoader + " (was: " + classLoaderOverride + ")");
        classLoaderOverride = classLoader;
        contextManagers = null;
        contextTimers = null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<ContextManager<?>> getContextManagers() {
        if (contextManagers == null) {
            synchronized (ContextManagers.class) {
                if (contextManagers == null) {
                    contextManagers = (List) load(ContextManager.class);
                }
            }
        }
        return contextManagers;
    }

    static List<ContextTimer> getContextTimers() {
        if (contextTimers == null) {
            synchronized (ContextManagers.class) {
                if (contextTimers == null) {
                    contextTimers = load(ContextTimer.class);
                }
            }
        }
        return contextTimers;
    }

    private static <T> List<T> load(Class<T> type) {
        ArrayList<T> list = new ArrayList<>();
        if (classLoaderOverride == null) {
            ServiceLoader.load(type).forEach(list::add);
        } else {
            ServiceLoader.load(type, classLoaderOverride).forEach(list::add);
        }
        list.trimToSize();
        return Collections.unmodifiableList(list);
    }

    /**
     * Implementation of the <code>createContextSnapshot</code> functionality that can reactivate all values from the
     * snapshot in each corresponding {@link ContextManager}.
     */
    @SuppressWarnings("rawtypes")
    private static final class ContextSnapshotImpl implements nl.talsmasoftware.context.api.ContextSnapshot {
        private static final ContextManager[] MANAGER_ARRAY = new ContextManager[0];
        private final ContextManager[] managers;
        private final Object[] values;

        private ContextSnapshotImpl(List<ContextManager<?>> managers, List<Object> values) {
            this.managers = managers.toArray(MANAGER_ARRAY);
            this.values = values.toArray();
        }

        public Reactivation reactivate() {
            final long start = System.nanoTime();
            final List<Context<?>> reactivatedContexts = new ArrayList<Context<?>>(managers.length);
            try {
                for (int i = 0; i < managers.length && i < values.length; i++) {
                    reactivatedContexts.add(reactivate(managers[i], values[i]));
                }
                ReactivationImpl reactivation = new ReactivationImpl(reactivatedContexts);
                Timers.timed(System.nanoTime() - start, nl.talsmasoftware.context.api.ContextSnapshot.class, "reactivate");
                return reactivation;
            } catch (RuntimeException reactivationException) {
                for (Context alreadyReactivated : reactivatedContexts) {
                    if (alreadyReactivated != null) try {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.finest("Snapshot reactivation failed! " +
                                    "Closing already reactivated context: " + alreadyReactivated + ".");
                        }
                        alreadyReactivated.close();
                    } catch (RuntimeException rte) {
                        reactivationException.addSuppressed(rte);
                    }
                }
                contextManagers = null;
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
            Timers.timed(System.nanoTime() - start, contextManager.getClass(), "initializeNewContext");
            return reactivated;
        }

        @Override
        public String toString() {
            return "ContextSnapshot{size=" + managers.length + '}';
        }
    }

    /**
     * Implementation of the reactivated 'container' context that closes all reactivated contexts
     * when it is closed itself.<br>
     * This context contains no meaningful value in itself and purely exists to close the reactivated contexts.
     */
    private static final class ReactivationImpl implements Reactivation {
        private final List<Context<?>> reactivated;

        private ReactivationImpl(List<Context<?>> reactivated) {
            this.reactivated = reactivated;
        }

        public void close() {
            RuntimeException closeException = null;
            // close in reverse order of reactivation
            for (int i = this.reactivated.size() - 1; i >= 0; i--) {
                Context<?> reactivated = this.reactivated.get(i);
                if (reactivated != null) try {
                    reactivated.close();
                } catch (RuntimeException rte) {
                    if (closeException == null) closeException = rte;
                    else closeException.addSuppressed(rte);
                }
            }
            if (closeException != null) throw closeException;
        }

        @Override
        public String toString() {
            return "ReactivatedContext{size=" + reactivated.size() + '}';
        }
    }

    /**
     * Exception that we don't actually throw, but it helps track the issue if we log it including the stacktrace.
     */
    private static class NoContextManagersFound extends RuntimeException {
        private NoContextManagersFound() {
            super("Context snapshot was created but no ContextManagers were found!"
                    + " Thread=" + Thread.currentThread()
                    + ", ContextClassLoader=" + Thread.currentThread().getContextClassLoader());
        }
    }
}
