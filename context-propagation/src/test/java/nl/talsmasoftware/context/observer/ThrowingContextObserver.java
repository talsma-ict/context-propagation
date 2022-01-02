/*
 * Copyright 2016-2022 Talsma ICT
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

import javax.annotation.Priority;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Priority(Integer.MIN_VALUE)
public class ThrowingContextObserver implements ContextObserver<Object> {
    static Class<? extends ContextManager<?>> observedContextManager = null;

    static final List<Observed> observed = new CopyOnWriteArrayList<Observed>();

    @SuppressWarnings("unchecked")
    public Class<? extends ContextManager<Object>> getObservedContextManager() {
        return (Class) observedContextManager;
    }

    public void onActivate(Object activatedContextValue, Object previousContextValue) {
        observed.add(new Observed(Observed.Action.ACTIVATE, activatedContextValue, previousContextValue));
        throw new UnsupportedOperationException("Method onActivate is not supported!");
    }

    public void onDeactivate(Object deactivatedContextValue, Object restoredContextValue) {
        observed.add(new Observed(Observed.Action.DEACTIVATE, deactivatedContextValue, restoredContextValue));
        throw new UnsupportedOperationException("Method onDeactivate is not supported!");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
