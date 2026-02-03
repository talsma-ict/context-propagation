/*
 * Copyright 2016-2026 Talsma ICT
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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet {@link Filter} that registers the current {@link ServletRequest} to become the
 * {@link ServletRequestContextManager#getActiveContextValue() active context value}
 * while the filter is active.
 *
 * @author Sjoerd Talsma
 */
public class ServletRequestContextFilter implements Filter {
    private static final Logger LOGGER = Logger.getLogger(ServletRequestContextFilter.class.getName());
    private final ContextManager<ServletRequest> manager;

    /**
     * Creates the servlet request context filter.
     */
    public ServletRequestContextFilter() {
        manager = ServletRequestContextManager.provider();
    }

    /**
     * Called by the web container to indicate to a filter that it is being placed into service.
     *
     * <p>
     * This filter does not need to initialize.
     *
     * @param filterConfig a {@code FilterConfig} object containing the filter's configuration and initialization parameters
     */
    @Override
    public void init(FilterConfig filterConfig) {
        // no-op
    }

    /**
     * Filter the request by activating a new {@linkplain Context} for the request, making sure to close it after
     * the request finishes.
     *
     * <p>
     * For {@linkplain ServletRequest#isAsyncStarted() asynchronous} requests,
     * an {@linkplain javax.servlet.AsyncListener AsyncListener} is added taking care of the servlet context
     * in the asynchronous handling.
     *
     * @param request  the <code>ServletRequest</code> object contains the client's request
     * @param response the <code>ServletResponse</code> object contains the filter's response
     * @param chain    the <code>FilterChain</code> for invoking the next filter or the resource
     * @throws IOException      In case an IO exception was thrown further down the filter chain.
     * @throws ServletException In case a servlet exception was thrown further down the filter chain.
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try (Context context = manager.activate(request)) {
            if (request.isAsyncStarted()) try {
                request.getAsyncContext().addListener(new ServletRequestContextAsyncListener(manager));
            } catch (IllegalStateException e) {
                LOGGER.log(Level.FINE, e, () -> "Could not register ServletRequest asynchronous listener: " + e.getMessage());
            }
            chain.doFilter(request, response);
        } finally {
            manager.clear(); // Make sure there are no requests returned to the HTTP thread pool.
        }
    }

    /**
     * Called by the web container to indicate to a filter that it is being taken out of service.
     *
     * <p>
     * This filter does not need to clean up before shutdown.
     */
    @Override
    public void destroy() {
        // no-op
    }

    /**
     * Returns a string representation of this filter.
     *
     * @return The simple class name of this servlet filter.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
