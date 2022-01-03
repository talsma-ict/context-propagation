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
package nl.talsmasoftware.context.clearable;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManager;

public class AutoInitializingContextManager implements ContextManager<String> {
    private static final ThreadLocal<DummyContext> CTX = new ThreadLocal<DummyContext>() {
        @Override
        protected DummyContext initialValue() {
            return new DummyContext(null, null);
        }
    };

    public Context<String> getActiveContext() {
        return CTX.get();
    }

    public Context<String> initializeNewContext(String value) {
        CTX.set(new DummyContext(CTX.get(), value));
        return CTX.get();
    }

    private static class DummyContext implements Context<String> {
        private final DummyContext previous;
        private String value;

        private DummyContext(DummyContext previous, String value) {
            this.previous = previous;
            this.value = value;
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
            if (previous == null) CTX.remove();
            else CTX.set(previous);
            this.value = null;
        }
    }
}
