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
package nl.talsmasoftware.context;

import java.util.Optional;

/**
 * Trivial manager around the {@link DummyContext} implementation to be registered as service provider.
 *
 * @author Sjoerd Talsma
 */
public class DummyContextManager implements ContextManager<String> {

    public Context<String> initializeNewContext(String value) {
        return setCurrentValue(value);
    }

    public Context<String> getActiveContext() {
        return DummyContext.current();
    }

    public static Optional<String> currentValue() {
        return Optional.ofNullable(DummyContext.currentValue());
    }

    /**
     * For easier testing
     *
     * @param value The new value to be set (can be null)
     * @return A context to optionally be closed
     */
    public static Context<String> setCurrentValue(String value) {
        return new DummyContext(value);
    }

    /**
     * For easier testing
     */
    public static void clear() {
        DummyContext.reset();
    }

    @Override
    public int hashCode() {
        return DummyContextManager.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof DummyContextManager;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
