package nl.talsmasoftware.context.servletrequest;

import nl.talsmasoftware.context.threadlocal.AbstractThreadLocalContext;

import javax.servlet.ServletRequest;

/**
 * ThreadLocal context containing the current {@link ServletRequest}.
 *
 * @author Sjoerd Talsma
 */
public class ServletRequestContext extends AbstractThreadLocalContext<ServletRequest> {
    /**
     * Constant for a dummy context that is already closed.
     */
    private static final ServletRequestContext DUMMY = new ServletRequestContext(null) {{
        super.close(); // Direct clean-up in static initialization-thread.
    }};

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
    public static ServletRequestContext current() {
        final ServletRequestContext current = CONTEXT.get();
        return current != null ? current : DUMMY;
    }

    /**
     * {@link #close()} restores the previous context, but clear() unconditionally removes the active context.
     * <p>
     * This is useful for boundary filters, whose Threads may be returned to some threadpool.
     */
    public static void clear() {
        CONTEXT.remove();
    }

}
