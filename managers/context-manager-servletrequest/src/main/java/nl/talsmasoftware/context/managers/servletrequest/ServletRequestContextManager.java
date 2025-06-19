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
package nl.talsmasoftware.context.managers.servletrequest;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;

import javax.servlet.ServletRequest;

/**
 * Manager to propagate the current {@linkplain javax.servlet.ServletRequest ServletRequest} from one thread to another.
 *
 * <p>
 * This propagates the context to other threads using the
 * {@code ContextAwareExecutorService} or {@code ContextAwareCompletableFuture}.
 *
 * <p>
 * Obtaining the {@linkplain ServletRequest} for the current thread can be
 * accomplished with the {@linkplain #currentServletRequest()} method.
 *
 * <p>
 * It should normally not be necessary to {@linkplain #activate(ServletRequest) activate new contexts}
 * from application code. Adding the {@linkplain ServletRequestContextFilter} to the filter chain should be enough to
 * have the {@linkplain #currentServletRequest() current servlet request} available in your application.
 *
 * <p>
 * All threads managed by an {@code ContextAwareExecutorService} or {@code ContextAwareCompletableFuture} automatically
 * propagates the context to other threads.<br>
 * Also, any function <em>..WithContext</em> in the {@code nl.talsmasoftware.context.core.function} package
 * will automatically activate the context snapshot around its function body.
 *
 * @author Sjoerd Talsma
 */
public final class ServletRequestContextManager implements ContextManager<ServletRequest> {
    /**
     * Singleton instance of this class.
     */
    @SuppressWarnings("java:S1874") // This is the singleton instance the constructor was deprecated for.
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
    @SuppressWarnings("java:S1133") // Code can only be removed if this library ever switches to Java 9+ compatibility.
    public ServletRequestContextManager() {
        super(); // no-op, default constructor for explicit deprecation.
    }

    /**
     * Static method to obtain the current {@link ServletRequest} (if available).
     *
     * @return The current ServletRequest if available, or {@code null} otherwise.
     */
    public static ServletRequest currentServletRequest() {
        return ServletRequestContext.currentValue();
    }

    /**
     * Activate a new {@linkplain ServletRequest} {@linkplain Context} for the given servlet request.
     *
     * <p>
     * In a normal application, it should not be necessary to call this explicitly.
     * Instead, the {@linkplain ServletRequestContextFilter} normally takes care of it.
     *
     * @param servletRequest The servlet request to become the 'current' request for this thread,
     *                       until the context is closed.
     * @return The context for the given servlet request.
     * The context <strong>must be</strong> closed in the same that activated it.
     */
    public Context activate(ServletRequest servletRequest) {
        return new ServletRequestContext(servletRequest);
    }

    /**
     * Returns the active {@linkplain ServletRequest} if any, otherwise {@code null}.
     *
     * @return The <em>active</em> servlet request.
     * @see #currentServletRequest()
     */
    public ServletRequest getActiveContextValue() {
        return currentServletRequest();
    }

    /**
     * Unconditionally removes the active context (and any parents).
     *
     * <p>
     * This is useful for boundary filters, whose Threads may be returned to some thread pool.
     *
     * <p>
     * The {@linkplain ServletRequestContextFilter} will clear the context after each request.
     */
    public void clear() {
        ServletRequestContext.clear();
    }

    /**
     * Returns a string representation for this context manager.
     *
     * @return The simple class name of this manager, as it contains no state of itself.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
