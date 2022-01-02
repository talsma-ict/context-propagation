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
package nl.talsmasoftware.context.servletrequest;

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
 * Servlet {@link Filter} that registers the current {@link ServletRequest} with the
 * {@link ServletRequestContextManager} to become the
 * {@link ServletRequestContextManager#getActiveContext() active context}
 * while the filter is active.
 *
 * @author Sjoerd Talsma
 */
public class ServletRequestContextFilter implements Filter {
    private static final Logger LOGGER = Logger.getLogger(ServletRequestContextFilter.class.getName());

    public void init(FilterConfig filterConfig) {
        // no-op
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {

            new ServletRequestContext(request); // Automatically becomes the new active context.
            if (request.isAsyncStarted()) try {
                request.getAsyncContext().addListener(new ServletRequestContextAsyncListener());
            } catch (IllegalStateException e) {
                LOGGER.log(Level.FINE, "Could not register servletrequest asynchronous listener: " + e.getMessage(), e);
            }

            chain.doFilter(request, response);

        } finally {
            ServletRequestContextManager.clear(); // Make sure there are no requests returned to the HTTP threadpool.
        }
    }

    public void destroy() {
        // no-op
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
