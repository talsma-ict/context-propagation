/*
 * Copyright (C) 2016 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.talsmasoftware.concurrency.context;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Sjoerd Talsma
 */
public final class DummyContext implements Context<String> {
    private static final ThreadLocal<DummyContext> INSTANCE = new InheritableThreadLocal<DummyContext>();

    private final DummyContext previous;
    private final String value;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public DummyContext(String newValue) {
        this.previous = INSTANCE.get();
        this.value = newValue;
        INSTANCE.set(this);
    }

    static void reset() {
        INSTANCE.remove();
    }

    static DummyContext current() {
        return INSTANCE.get();
    }

    static String currentValue() {
        final DummyContext current = current();
        return current != null ? current.getValue() : null;
    }

    boolean isClosed() {
        return closed.get();
    }

    public String getValue() {
        return closed.get() ? null : value;
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            DummyContext current = INSTANCE.get();
            if (this == current) {
                while (current.closed.get()) {
                    current = current.previous;
                    if (current == null) {
                        INSTANCE.remove();
                        return;
                    }
                }
                INSTANCE.set(current);
            }
        }
    }
}
