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
package nl.talsmasoftware.context.manager.servletrequest;

import nl.talsmasoftware.context.api.Context;

import javax.servlet.ServletRequest;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple context using a ThreadLocal containing the current {@link ServletRequest}.
 *
 * @author Sjoerd Talsma
 */
final class ServletRequestContext implements Context<ServletRequest> {
    private static final ThreadLocal<ServletRequestContext> CONTEXT = new ThreadLocal<>();
    final ServletRequestContext previous;
    final ServletRequest value;
    final AtomicBoolean closed;

    ServletRequestContext(ServletRequest servletRequest) {
        previous = CONTEXT.get();
        value = servletRequest;
        closed = new AtomicBoolean(false);
        CONTEXT.set(this);
    }

    @Override
    public ServletRequest getValue() {
        return closed.get() ? null : value;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            CONTEXT.set(previous);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + (closed.get() ? "{closed}" : "{value=" + value + '}');
    }

    static ServletRequest currentValue() {
        ServletRequestContext current = CONTEXT.get();
        return current != null ? current.value : null;
    }

    static void clear() {
        try {
            for (ServletRequestContext current = CONTEXT.get(); current != null; current = CONTEXT.get()) {
                current.close();
            }
        } finally {
            CONTEXT.remove();
        }
    }

}
