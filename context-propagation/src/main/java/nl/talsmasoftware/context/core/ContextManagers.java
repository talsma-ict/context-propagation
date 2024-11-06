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
import nl.talsmasoftware.context.api.ContextObserver;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.api.ContextSnapshot.Reactivation;
import nl.talsmasoftware.context.core.delegation.Wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
     * The service loader that loads (and possibly caches) {@linkplain ContextManager} instances in prioritized order.
     */
    private static final PriorityServiceLoader<ContextManager> CONTEXT_MANAGERS =
            new PriorityServiceLoader<>(ContextManager.class);

    /**
     * Registered observers.
     */
    private static final CopyOnWriteArrayList<ObservableContextManager> OBSERVERS =
            new CopyOnWriteArrayList<>();

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
                CONTEXT_MANAGERS.clearCache();
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
     * Register an observer for contexts managed by the specified ContextManager type.
     *
     * @param contextObserver            The observer to register.
     * @param observedContextManagerType The context manager type to observe.
     * @param <T>                        Type of the value in the context.
     * @return {@code true} if the observer was registered.
     * @since 1.1.0
     */
    public static <T> boolean registerContextObserver(ContextObserver<? super T> contextObserver, Class<? extends ContextManager<T>> observedContextManagerType) {
        if (contextObserver == null) {
            throw new NullPointerException("Context observer must not be null.");
        } else if (observedContextManagerType == null) {
            throw new NullPointerException("Observed ContextManager type must not be null.");
        }

        // Find ContextManager to register.
        ObservableContextManager<T> observableContextManager = null;
        ContextManager<T> contextManager = null;
        for (ContextManager<?> manager : getContextManagers()) {
            if (manager instanceof ObservableContextManager
                    && ((ObservableContextManager<?>) manager).observes(observedContextManagerType)) {
                observableContextManager = (ObservableContextManager<T>) manager;
                break;
            } else if (observedContextManagerType.isInstance(manager)) {
                contextManager = (ContextManager<T>) manager;
                break;
            }
        }
        if (observableContextManager == null && contextManager == null) {
            LOGGER.warning("Trying to register observer to missing ContextManager type: " + observedContextManagerType + ".");
            return false;
        }

        if (observableContextManager == null) {
            // Register new observer by wrapping the context manager.
            ObservableContextManager<T> newObserver = new ObservableContextManager<T>(contextManager, (List) Arrays.asList(contextObserver));
            if (OBSERVERS.addIfAbsent(newObserver)) {
                return true;
            }

            // There is already an existing ObservableContextManager, add the observer to it.
            observableContextManager = OBSERVERS.get(OBSERVERS.indexOf(newObserver));
        }

        // Add the context observer to the existing observable context manager.
        synchronized (observableContextManager) {
            return observableContextManager.observers.addIfAbsent(contextObserver);
        }
    }

    /**
     * Unregister an observer for any context.
     *
     * @param contextObserver The previously registered context observer.
     * @return {@code true} if the observer was unregistered.
     * @since 1.1.0
     */
    public static boolean unregisterContextObserver(ContextObserver<?> contextObserver) {
        boolean unregistered = false;
        for (ObservableContextManager<?> observer : OBSERVERS) {
            unregistered |= observer.observers.remove(contextObserver);
            synchronized (observer) {
                if (observer.observers.isEmpty()) {
                    OBSERVERS.remove(observer);
                }
            }
        }
        return unregistered;
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
     * {@linkplain nl.talsmasoftware.context.observer.ContextObserver context observers}
     * </ul>
     *
     * @param classLoader The single, fixed ClassLoader to use for finding context managers.
     *                    Specify {@code null} to restore the default behaviour.
     * @since 1.0.5
     */
    public static void useClassLoader(ClassLoader classLoader) {
        Level loglevel = PriorityServiceLoader.classLoaderOverride == classLoader ? Level.FINEST : Level.FINE;
        if (LOGGER.isLoggable(loglevel)) {
            LOGGER.log(loglevel, "Setting override classloader for loading ContextManager and ContextObserver " +
                    "instances to " + classLoader + " (was: " + PriorityServiceLoader.classLoaderOverride + ").");
        }
        PriorityServiceLoader.classLoaderOverride = classLoader;
    }

    private static Iterable<ContextManager> getContextManagers() {
        // TODO change to stream implementation when java 8
        return OBSERVERS.isEmpty() ? CONTEXT_MANAGERS : new Iterable<ContextManager>() {
            public Iterator<ContextManager> iterator() {
                return new Iterator<ContextManager>() {
                    private final Iterator<ContextManager> delegate = CONTEXT_MANAGERS.iterator();

                    public boolean hasNext() {
                        return delegate.hasNext();
                    }

                    public ContextManager next() {
                        ContextManager contextManager = delegate.next();
                        if (!(contextManager instanceof ObservableContextManager)) {
                            for (ObservableContextManager observableContextManager : OBSERVERS) {
                                if (observableContextManager.isWrapperOf(contextManager)) {
                                    CONTEXT_MANAGERS.replaceInCache(contextManager, observableContextManager);
                                    return observableContextManager;
                                }
                            }
                        }
                        return contextManager;
                    }

                    public void remove() {
                        delegate.remove();
                    }
                };
            }
        };
    }

    /**
     * Implementation of the <code>createContextSnapshot</code> functionality that can reactivate all values from the
     * snapshot in each corresponding {@link ContextManager}.
     */
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
                CONTEXT_MANAGERS.clearCache();
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

    private static final class ObservableContextManager<T> extends Wrapper<ContextManager<T>> implements ContextManager<T> {
        private final CopyOnWriteArrayList<ContextObserver<? super T>> observers;

        private ObservableContextManager(ContextManager<T> delegate, List<ContextObserver<? super T>> observers) {
            super(delegate);
            this.observers = new CopyOnWriteArrayList<ContextObserver<? super T>>(observers);
        }

        private boolean observes(Class<? extends ContextManager<?>> contextManagerType) {
            return contextManagerType.isInstance(delegate());
        }

        @Override
        public T getActiveContextValue() {
            return delegate().getActiveContextValue();
        }

        @Override
        public void clear() {
            delegate().clear();
        }

        private void notifyActivated(T newValue, T oldValue) {
            for (ContextObserver<? super T> observer : observers) {
                try {
                    observer.onActivate(newValue, oldValue);
                } catch (RuntimeException observerError) {
                    LOGGER.log(Level.SEVERE, "Error in observer.onActivate of " + observer, observerError);
                }
            }
        }

        private void notifyDeactivated(T deactivatedValue, T restoredValue) {
            for (ContextObserver<? super T> observer : observers) {
                try {
                    observer.onDeactivate(deactivatedValue, restoredValue);
                } catch (RuntimeException observerError) {
                    LOGGER.log(Level.SEVERE, "Error in observer.onActivate of " + observer, observerError);
                }
            }
        }

        @Override
        public Context<T> initializeNewContext(final T newValue) {
            final T oldValue = getActiveContextValue();
            final Context<T> context = delegate().initializeNewContext(newValue);
            notifyActivated(newValue, oldValue);

            return new Context<T>() {
                public T getValue() {
                    return context.getValue();
                }

                public void close() {
                    T deactivated = context.getValue(); // get before closing!
                    context.close();
                    notifyDeactivated(deactivated, getActiveContextValue());
                }
            };
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '{' + delegate() + ", " + observers + '}';
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
