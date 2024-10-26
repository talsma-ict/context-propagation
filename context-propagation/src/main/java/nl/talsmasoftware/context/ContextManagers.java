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
package nl.talsmasoftware.context;

import nl.talsmasoftware.context.api.ContextObserver;
import nl.talsmasoftware.context.clearable.Clearable;
import nl.talsmasoftware.context.delegation.Wrapper;

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
 * @see nl.talsmasoftware.context.core.ContextManagers
 * @deprecated This class has moved to the {@code nl.talsmasoftware.context.core} package.
 */
@Deprecated
public final class ContextManagers {
    private static final Logger LOGGER = Logger.getLogger(ContextManagers.class.getName());

    /**
     * The service loader that loads (and possibly caches) {@linkplain ContextManager} instances in prioritized order.
     */
    private static final PriorityServiceLoader<ContextManager> CONTEXT_MANAGERS =
            new PriorityServiceLoader<ContextManager>(ContextManager.class);

    /**
     * Registered observers.
     */
    private static final CopyOnWriteArrayList<ObservableContextManager> OBSERVERS =
            new CopyOnWriteArrayList<ObservableContextManager>();

    /**
     * The service loader that loads (and possibly caches) {@linkplain ContextManager} instances in prioritized order.
     *
     * @deprecated To be replaced by explicitly registered {@link #OBSERVERS}
     */
    @Deprecated
    private static final PriorityServiceLoader<nl.talsmasoftware.context.observer.ContextObserver> CONTEXT_OBSERVERS =
            new PriorityServiceLoader<nl.talsmasoftware.context.observer.ContextObserver>(nl.talsmasoftware.context.observer.ContextObserver.class);

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
     *
     * @return A new snapshot that can be reactivated elsewhere (e.g. a background thread or even another node)
     * within a try-with-resources construct.
     * @deprecated Moved to the {@code nl.talsmasoftware.context.core} package.
     */
    @Deprecated
    public static nl.talsmasoftware.context.api.ContextSnapshot createContextSnapshot() {
        return nl.talsmasoftware.context.core.ContextManagers.createContextSnapshot();
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
     *
     * @deprecated Moved to the {@code nl.talsmasoftware.context.core} package.
     */
    @Deprecated
    public static void clearActiveContexts() {
        nl.talsmasoftware.context.core.ContextManagers.clearActiveContexts();
    }

    /**
     * Register an observer for contexts managed by the specified ContextManager type.
     *
     * @param contextObserver            The observer to register.
     * @param observedContextManagerType The context manager type to observe.
     * @param <T>                        Type of the value in the context.
     * @return {@code true} if the observer was registered.
     * @since 1.1.0
     * @deprecated Moved to the {@code nl.talsmasoftware.context.core} package.
     */
    @Deprecated
    public static <T> boolean registerContextObserver(ContextObserver<? super T> contextObserver, Class<ContextManager<T>> observedContextManagerType) {
        return nl.talsmasoftware.context.core.ContextManagers.registerContextObserver(contextObserver, observedContextManagerType);
    }

    /**
     * Unregister an observer for any context.
     *
     * @param contextObserver The previously registered context observer.
     * @return {@code true} if the observer was unregistered.
     * @since 1.1.0
     * @deprecated Moved to the {@code nl.talsmasoftware.context.core} package.
     */
    @Deprecated
    public static boolean unregisterContextObserver(ContextObserver<?> contextObserver) {
        return nl.talsmasoftware.context.core.ContextManagers.unregisterContextObserver(contextObserver);
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
     * @deprecated Moved to the {@code nl.talsmasoftware.context.core} package.
     */
    @Deprecated
    public static void useClassLoader(ClassLoader classLoader) {
        PriorityServiceLoader.classLoaderOverride = classLoader;
        nl.talsmasoftware.context.core.ContextManagers.useClassLoader(classLoader);
    }

    /**
     * Notify all {@linkplain ContextObserver context observers} for the specified {@code contextManager}
     * about the activated context value.
     *
     * @param contextManager        The context manager type that activated the context (required to observe).
     * @param activatedContextValue The activated context value or {@code null} if no value was activated.
     * @param previousContextValue  The previous context value or {@code null} if unknown or unsupported.
     * @param <T>                   The type managed by the context manager.
     * @see nl.talsmasoftware.context.core.ContextManagers#registerContextObserver(ContextObserver, Class)
     * @see nl.talsmasoftware.context.core.ContextManagers#unregisterContextObserver(ContextObserver)
     * @since 1.0.6
     * @deprecated Replaced by explicit observer registration
     */
    @Deprecated
    @SuppressWarnings("unchecked") // If the observer tells us it can observe the values, we trust it.
    public static <T> void onActivate(Class<? extends ContextManager<? super T>> contextManager,
                                      T activatedContextValue,
                                      T previousContextValue) {
        if (contextManager != null) {
            for (nl.talsmasoftware.context.observer.ContextObserver observer : CONTEXT_OBSERVERS) {
                try {
                    final Class observedContext = observer.getObservedContextManager();
                    // TODO unwrap ObservableContextManager.type and/or skip
                    if (observedContext != null && observedContext.isAssignableFrom(contextManager)) {
                        observer.onActivate(activatedContextValue, previousContextValue);
                        // TODO log warning about deprecated mechanism
                    }
                } catch (RuntimeException observationException) {
                    Logger.getLogger(observer.getClass().getName()).log(Level.WARNING,
                            "Exception in " + observer.getClass().getSimpleName()
                                    + ".onActivate(" + activatedContextValue + ", " + previousContextValue
                                    + ") for " + contextManager.getSimpleName() + ": " + observationException.getMessage(),
                            observationException);
                }
            }
        }
    }

    /**
     * Notify all {@linkplain ContextObserver context observers} for the specified {@code contextManager}
     * about the deactivated context value.
     *
     * @param contextManager          The context manager type that deactivated the context (required to observe).
     * @param deactivatedContextValue The deactivated context value
     * @param restoredContextValue    The restored context value or {@code null} if unknown or unsupported.
     * @param <T>                     The type managed by the context manager.
     * @since 1.0.6
     * @deprecated To be replaced by explicit observer registration method (TODO)
     */
    @Deprecated
    @SuppressWarnings("unchecked") // If the observer tells us it can observe the values, we trust it.
    public static <T> void onDeactivate(Class<? extends ContextManager<? super T>> contextManager,
                                        T deactivatedContextValue,
                                        T restoredContextValue) {
        if (contextManager != null) {
            for (nl.talsmasoftware.context.observer.ContextObserver observer : CONTEXT_OBSERVERS) {
                try {
                    final Class observedContext = observer.getObservedContextManager();
                    // TODO unwrap ObservableContextManager.type and/or skip
                    if (observedContext != null && observedContext.isAssignableFrom(contextManager)) {
                        observer.onDeactivate(deactivatedContextValue, restoredContextValue);
                        // TODO log warning about deprecated mechanism
                    }
                } catch (RuntimeException observationException) {
                    Logger.getLogger(observer.getClass().getName()).log(Level.WARNING,
                            "Exception in " + observer.getClass().getSimpleName()
                                    + ".onDeactivate(" + deactivatedContextValue + ", " + deactivatedContextValue
                                    + ") for " + contextManager.getSimpleName() + ": " + observationException.getMessage(),
                            observationException);
                }
            }
        }
    }

    private static final class ObservableContextManager<T> extends Wrapper<ContextManager<T>> implements ContextManager<T> {
        private final CopyOnWriteArrayList<ContextObserver<? super T>> observers;

        private ObservableContextManager(ContextManager<T> delegate, List<ContextObserver<? super T>> observers) {
            super(delegate);
            this.observers = new CopyOnWriteArrayList<ContextObserver<? super T>>(observers);
        }

        private T getActiveContextValue() {
            final Context<T> activeContext = delegate().getActiveContext();
            return activeContext != null ? activeContext.getValue() : null;
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

        @Deprecated
        public Context<T> getActiveContext() {
            return delegate().getActiveContext();
        }

        @Override
        public int hashCode() {
            return delegate().hashCode();
        }

        @Override
        public boolean equals(Object other) { // IMPORTANT to reliably register observers.
            return other instanceof ObservableContextManager
                    && delegate().equals(((ObservableContextManager<?>) other).delegate());
        }

        @Override
        public String toString() {
            return delegate().toString();
        }
    }
}
