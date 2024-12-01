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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Sjoerd Talsma
 */
public final class DummyContext implements Context<String> {
    private static final ThreadLocal<DummyContext> INSTANCE = new ThreadLocal<>();

    private final DummyContext parent;
    private final String value;
    private final AtomicBoolean closed;

    public DummyContext(String newValue) {
        this.parent = INSTANCE.get();
        this.value = newValue;
        this.closed = new AtomicBoolean(false);
        INSTANCE.set(this);
    }

    // Public for testing!
    public boolean isClosed() {
        return closed.get();
    }

    public String getValue() {
        return value;
    }

    public void close() {
        if (closed.compareAndSet(false, true) && INSTANCE.get() == this) {
            DummyContext current = INSTANCE.get();
            while (current != null && current.isClosed()) {
                current = current.parent;
            }
            if (current == null) {
                INSTANCE.remove();
            } else {
                INSTANCE.set(current);
            }
        }
    }

    public static String currentValue() {
        final Context<String> currentContext = INSTANCE.get();
        return currentContext != null ? currentContext.getValue() : null;
    }

    public static void setCurrentValue(String value) {
        new DummyContext(value);
    }

    public static void reset() {
        INSTANCE.remove();
    }

}
