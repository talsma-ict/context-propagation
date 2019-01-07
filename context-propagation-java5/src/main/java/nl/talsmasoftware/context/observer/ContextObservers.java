/*
 * Copyright 2016-2019 Talsma ICT
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
package nl.talsmasoftware.context.observer;

import nl.talsmasoftware.context.ContextManager;
import nl.talsmasoftware.context.PriorityServiceLoader;

public final class ContextObservers {

    /**
     * The service loader that loads (and possibly caches) {@linkplain ContextManager} instances in prioritized order.
     */
    private static final PriorityServiceLoader<ContextObserver> CONTEXT_OBSERVERS =
            new PriorityServiceLoader<ContextObserver>(ContextObserver.class);

    /**
     * Private constructor to avoid instantiation of this class.
     */
    private ContextObservers() {
        throw new UnsupportedOperationException();
    }

    public static <T> void onActivate(Class<? extends ContextManager<? super T>> contextManager,
                                      T activatedContextValue,
                                      T previousContextValue) {
        if (contextManager != null) for (ContextObserver observer : CONTEXT_OBSERVERS) {
            final Class observedContext = observer.getObservedContext();
            if (observedContext != null && observedContext.isAssignableFrom(contextManager)) {
                observer.onActivate(activatedContextValue, previousContextValue);
            }
        }
    }

    public static <T> void onDeactivate(Class<? extends ContextManager<? super T>> contextManager,
                                        T deactivatedContextValue,
                                        T restoredContextValue) {
        if (contextManager != null) for (ContextObserver observer : CONTEXT_OBSERVERS) {
            final Class observedContext = observer.getObservedContext();
            if (observedContext != null && observedContext.isAssignableFrom(contextManager)) {
                observer.onDeactivate(deactivatedContextValue, restoredContextValue);
            }
        }
    }

}
