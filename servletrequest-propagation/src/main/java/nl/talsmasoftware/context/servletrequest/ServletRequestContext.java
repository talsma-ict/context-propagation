/*
 * Copyright 2016-2018 Talsma ICT
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
package nl.talsmasoftware.context.servletrequest;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.threadlocal.AbstractThreadLocalContext;

import javax.servlet.ServletRequest;

/**
 * ThreadLocal context containing the current {@link ServletRequest}.
 *
 * @author Sjoerd Talsma
 */
final class ServletRequestContext extends AbstractThreadLocalContext<ServletRequest> {

    /**
     * The ThreadLocal context containing the {@link ServletRequest}.
     */
    private static final ThreadLocal<ServletRequestContext> CONTEXT = threadLocalInstanceOf(ServletRequestContext.class);

    /**
     * Creates a new context with the specified request.
     * <p>
     * The new context will be made the active context for the current thread.
     *
     * @param newValue The new value to become active in this new context
     *                 (or <code>null</code> to register a new context with 'no value').
     */
    ServletRequestContext(ServletRequest newValue) {
        super(newValue);
    }

    /**
     * The current servlet request context.
     * <p>
     * If no active {@link ServletRequest} is found, a 'dummy', already-closed context is returned.
     *
     * @return The context with the current ServletRequest.
     * The context itself is non-<code>null</code>, but may contain a <code>null</code> value.
     * @see #getValue()
     */
    static Context<ServletRequest> current() {
        final ServletRequestContext current = CONTEXT.get();
        return current != null ? current : None.INSTANCE;
    }

    /**
     * {@link #close()} restores the previous context, but clear() unconditionally removes the active context.
     * <p>
     * This is useful for boundary filters, whose Threads may be returned to some threadpool.
     */
    static void clear() {
        CONTEXT.remove();
    }

    /**
     * Dummy context containing no servlet request.
     */
    private static final class None implements Context<ServletRequest> {
        private static final None INSTANCE = new None();

        public ServletRequest getValue() {
            return null;
        }

        public void close() {
        }

        @Override
        public String toString() {
            return "ServletRequestContext.None";
        }
    }
}
