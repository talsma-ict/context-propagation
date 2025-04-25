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
package nl.talsmasoftware.context.clearable;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;

public class ClearableDummyContextManager implements ContextManager<String> {
    private static final ThreadLocal<DummyContext> CTX = new ThreadLocal<DummyContext>();

    public String getActiveContextValue() {
        DummyContext current = CTX.get();
        return current != null ? current.getValue() : null;
    }

    public Context initializeNewContext(String value) {
        return new DummyContext(value);
    }

    public void clear() {
        CTX.remove();
    }

    private static class DummyContext implements Context {
        private final DummyContext previous;
        private String value;

        private DummyContext(String value) {
            this.previous = CTX.get();
            this.value = value;
            CTX.set(this);
        }

        public String getValue() {
            return value;
        }

        /**
         * Only for testing! DO NOT COPY! This is not a safe implementation.
         * <p>
         * Please see AbstractThreadLocalContext for reference code if you wish manage a stack of contexts!
         */
        public void close() {
            CTX.set(previous);
            this.value = null;
        }
    }
}
