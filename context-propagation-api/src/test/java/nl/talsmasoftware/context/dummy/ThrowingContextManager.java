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
package nl.talsmasoftware.context.dummy;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Badly behaved {@link ContextManager} implementation that can throw things at us for testing purposes.
 *
 * @author Sjoerd Talsma
 */
public class ThrowingContextManager implements ContextManager<String> {
    public static RuntimeException inConstructor = null, onInitialize = null, onGet = null, onClose = null, onClear = null;

    public ThrowingContextManager() {
        if (inConstructor != null) try {
            throw inConstructor;
        } finally {
            inConstructor = null;
        }
    }

    @Override
    public Context<String> initializeNewContext(String value) {
        if (onInitialize != null) try {
            throw onInitialize;
        } finally {
            onInitialize = null;
        }
        return new Ctx(value);
    }

    @Override
    public String getActiveContextValue() {
        if (onGet != null) try {
            throw onGet;
        } finally {
            onGet = null;
        }
        return Ctx.currentValue();
    }

    @Override
    public void clear() {
        if (onClear != null) try {
            throw onClear;
        } finally {
            onClear = null;
        }
        Ctx.remove();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private final static class Ctx implements Context<String> {
        private static final ThreadLocal<Ctx> STORAGE = new ThreadLocal<>();

        private final Ctx parent;
        private final String value;
        private final AtomicBoolean closed;

        private Ctx(String newValue) {
            parent = STORAGE.get();
            value = newValue;
            closed = new AtomicBoolean(false);
            STORAGE.set(this);
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public void close() {
            if (onClose != null) try {
                throw onClose;
            } finally {
                onClose = null;
            }
            if (closed.compareAndSet(false, true) && STORAGE.get() == this) {
                Ctx current = STORAGE.get();
                while (current != null && current.closed.get()) {
                    current = current.parent;
                }
                if (current == null) {
                    STORAGE.remove();
                } else {
                    STORAGE.set(current);
                }
            }
        }

        private static String currentValue() {
            Ctx current = STORAGE.get();
            return current != null ? current.getValue() : null;
        }

        private static void remove() {
            STORAGE.remove();
        }
    }
}
