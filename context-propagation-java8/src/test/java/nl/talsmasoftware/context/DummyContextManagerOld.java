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

import nl.talsmasoftware.context.threadlocal.AbstractThreadLocalContext;

import java.util.Optional;
import java.util.logging.Logger;

public class DummyContextManagerOld implements ContextManager<String> {
    private static final Logger LOGGER = Logger.getLogger(DummyContextManagerOld.class.getName());

    public Context<String> initializeNewContext(String value) {
        return setCurrentValue(value);
    }

    public Context<String> getActiveContext() {
        return DummyContext.current();
    }

    public static Optional<String> currentValue() {
        Optional<String> currentValue = Optional.ofNullable(DummyContext.current()).map(Context::getValue);
        LOGGER.fine(() -> "Current value in " + Thread.currentThread() + ": " + currentValue);
        return currentValue;
    }

    /**
     * For easier testing
     *
     * @param value The new value to be set (can be null)
     * @return A context to optionally be closed
     */
    public static Context<String> setCurrentValue(String value) {
        LOGGER.fine(() -> "Setting current value in " + Thread.currentThread() + ": " + value);
        return new DummyContext(value);
    }

    /**
     * For easier testing
     */
    public static void clear() {
        LOGGER.fine(() -> "Clearing values in " + Thread.currentThread() + ", currently: " + DummyContext.current());
        DummyContext.clear();
    }

    private static final class DummyContext extends AbstractThreadLocalContext<String> {
        private DummyContext(String newValue) {
            super(DummyContextManagerOld.class, newValue);
        }

        private static void clear() {
            AbstractThreadLocalContext.threadLocalInstanceOf(DummyContext.class).remove();
        }

        private static Context<String> current() {
            return AbstractThreadLocalContext.current(DummyContext.class);
        }
    }
}