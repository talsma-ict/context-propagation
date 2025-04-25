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

import nl.talsmasoftware.context.core.threadlocal.AbstractThreadLocalContext;

/**
 * @author Sjoerd Talsma
 */
public final class DummyContext extends AbstractThreadLocalContext<String> {
    private static final ThreadLocal<DummyContext> INSTANCE =
            threadLocalInstanceOf(DummyContext.class);

    public DummyContext(String newValue) {
        super(newValue);
    }

    // Public for testing!
    public boolean isClosed() {
        return super.isClosed();
    }

    public static String currentValue() {
        final DummyContext currentContext = INSTANCE.get();
        return currentContext != null ? currentContext.value : null;
    }

    public static void setCurrentValue(String value) {
        new DummyContext(value);
    }

    public static void reset() {
        INSTANCE.remove();
    }

}
