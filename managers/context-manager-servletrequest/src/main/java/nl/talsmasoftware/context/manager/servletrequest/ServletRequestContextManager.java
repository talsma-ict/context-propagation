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
import nl.talsmasoftware.context.api.ContextManager;

import javax.servlet.ServletRequest;

/**
 * Manager to propagate a current {@link ServletRequest} with.
 *
 * @author Sjoerd Talsma
 */
public final class ServletRequestContextManager implements ContextManager<ServletRequest> {
    /**
     * Singleton instance of this class.
     */
    private static final ServletRequestContextManager INSTANCE = new ServletRequestContextManager();

    /**
     * Returns the singleton instance of the {@code ServletRequestContextManager}.
     *
     * <p>
     * The ServiceLoader supports a static {@code provider()} method to resolve services since Java 9.
     *
     * @return The context manager for ServletRequests.
     */
    public static ServletRequestContextManager provider() {
        return INSTANCE;
    }

    /**
     * Creates a new context manager.
     *
     * @see #provider()
     * @deprecated This constructor only exists for usage by Java 8 {@code ServiceLoader}. The singleton instance
     * obtained from {@link #provider()} should be used to avoid unnecessary instantiations.
     */
    @Deprecated
    public ServletRequestContextManager() {
    }

    /**
     * Static utility method to obtain the current {@link ServletRequest} (if available).
     *
     * @return The current ServletRequest if available, or <code>null</code> otherwise.
     */
    public static ServletRequest currentServletRequest() {
        return ServletRequestContext.currentValue();
    }

    public Context<ServletRequest> initializeNewContext(ServletRequest value) {
        return new ServletRequestContext(value);
    }

    public ServletRequest getActiveContextValue() {
        return currentServletRequest();
    }

    /**
     * Unconditionally removes the active context (and any parents).
     * <p>
     * This is useful for boundary filters, whose Threads may be returned to some thread pool.
     */
    public void clear() {
        ServletRequestContext.clear();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
