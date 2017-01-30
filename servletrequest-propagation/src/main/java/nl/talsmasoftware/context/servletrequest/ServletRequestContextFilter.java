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
