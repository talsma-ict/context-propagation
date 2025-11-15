/*
 * Copyright 2016-2025 Talsma ICT
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
public class ThrowingContextManager implements ContextManager<Object> {
    public static RuntimeException inConstructor = null, onActivate = null, onGet = null, onClose = null, onClear = null;

    public ThrowingContextManager() {
        if (inConstructor != null) try {
            throw inConstructor;
        } finally {
            inConstructor = null;
        }
    }

    @Override
    public Context activate(Object value) {
        if (onActivate != null) try {
            throw onActivate;
        } finally {
            onActivate = null;
        }
        return new Ctx(value);
    }

    @Override
    public Object getActiveContextValue() {
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
    public int hashCode() {
        return ThrowingContextManager.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ThrowingContextManager;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private static final class Ctx implements Context {
        private static final ThreadLocal<Ctx> STORAGE = new ThreadLocal<>();

        private final Ctx parent;
        private final Object value;
        private final AtomicBoolean closed;

        private Ctx(Object newValue) {
            parent = STORAGE.get();
            value = newValue;
            closed = new AtomicBoolean(false);
            STORAGE.set(this);
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

        private static Object currentValue() {
            Ctx current = STORAGE.get();
            return current != null ? current.value : null;
        }

        private static void remove() {
            STORAGE.remove();
        }
    }
}
