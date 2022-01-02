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
package nl.talsmasoftware.context;

import nl.talsmasoftware.context.threadlocal.AbstractThreadLocalContext;

/**
 * Badly behaved {@link ContextManager} implementation that can throw things at us for testing purposes.
 *
 * @author Sjoerd Talsma
 */
public class ThrowingContextManager implements ContextManager<String> {
    public static RuntimeException inConstructor = null, onInitialize = null, onGet = null, onClose = null;

    public ThrowingContextManager() {
        if (inConstructor != null) try {
            throw inConstructor;
        } finally {
            inConstructor = null;
        }
    }

    public Context<String> initializeNewContext(String value) {
        if (onInitialize != null) try {
            throw onInitialize;
        } finally {
            onInitialize = null;
        }
        return new Ctx(value);
    }

    public Context<String> getActiveContext() {
        if (onGet != null) try {
            throw onGet;
        } finally {
            onGet = null;
        }
        return Ctx.current();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private final static class Ctx extends AbstractThreadLocalContext<String> {
        private Ctx(String newValue) {
            super(ThrowingContextManager.class, newValue);
        }

        private static Ctx current() {
            return current(Ctx.class);
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
