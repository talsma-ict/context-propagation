/*
 * Copyright 2016-2017 Talsma ICT
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

import javax.servlet.*;
import java.io.IOException;

/**
 * Servlet {@link Filter} that registers the current {@link ServletRequest} with the
 * {@link ServletRequestContextManager} to become the
 * {@link ServletRequestContextManager#getActiveContext() active context}
 * while the filter is active.
 *
 * @author Sjoerd Talsma
 */
public class ServletRequestContextFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {
        // no-op
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            new ServletRequestContext(request); // Automatically becomes the new active context.
            chain.doFilter(request, response);
        } finally {
            ServletRequestContext.clear(); // Make sure there are no requests returned to the HTTP threadpool.
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
