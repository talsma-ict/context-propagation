/*
 * Copyright 2016-2026 Talsma ICT
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
import nl.talsmasoftware.context.core.threadlocal.AbstractThreadLocalContext;

/**
 * Badly behaved {@link ContextManager} implementation that can throw things at us for testing purposes.
 *
 * @author Sjoerd Talsma
 */
public class ThrowingContextManager implements ContextManager<String> {
    public static RuntimeException inConstructor = null, onActivate = null, onGet = null, onClose = null, onClear = null;

    public ThrowingContextManager() {
        if (inConstructor != null) try {
            throw inConstructor;
        } finally {
            inConstructor = null;
        }
    }

    @Override
    public Context activate(String value) {
        if (onActivate != null) try {
            throw onActivate;
        } finally {
            onActivate = null;
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

    private static final class Ctx extends AbstractThreadLocalContext<String> {
        private Ctx(String newValue) {
            super(newValue);
        }

        private static String currentValue() {
            Ctx current = AbstractThreadLocalContext.current(Ctx.class);
            return current != null ? current.value : null;
        }

        private static void remove() {
            AbstractThreadLocalContext.threadLocalInstanceOf(Ctx.class).remove();
        }

        @Override
        public void close() {
            if (onClose != null) try {
                throw onClose;
            } finally {
                onClose = null;
            }
            super.close();
        }
    }
}
