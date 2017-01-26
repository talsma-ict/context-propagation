package nl.talsmasoftware.context.servletrequest;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManager;

import javax.servlet.ServletRequest;

/**
 * Manager to propagate a current {@link ServletRequest} with.
 *
 * @author Sjoerd Talsma
 */
public final class ServletRequestContextManager implements ContextManager<ServletRequest> {

    public Context<ServletRequest> initializeNewContext(ServletRequest value) {
        return new ServletRequestContext(value);
    }

    public Context<ServletRequest> getActiveContext() {
        return ServletRequestContext.current();
    }

    /**
     * Unconditionally removes the active context (and any parents).
     * <p>
     * This is useful for boundary filters, whose Threads may be returned to some threadpool.
     */
    public static void clear() {
        ServletRequestContext.clear();
    }

    public String toString() {
        return "ServletRequestContextManager";
    }

}
