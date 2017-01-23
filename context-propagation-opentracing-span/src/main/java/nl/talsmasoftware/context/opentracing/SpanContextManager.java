package nl.talsmasoftware.context.opentracing;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.DefaultSpanManager;
import io.opentracing.contrib.spanmanager.SpanManager;
import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManager;

/**
 * {@link ContextManager} implementation for OpenTracing {@link Span} objects
 * delegating to an existing {@link SpanManager} implementation.
 * <p>
 * This adds {@link DefaultSpanManager} support to the
 * global {@link nl.talsmasoftware.context.ContextManagers#createContextSnapshot() context snapshot}.
 * <p>
 * This functionality is automatically added to the {@link nl.talsmasoftware.context.ContextManagers} class when
 * the service file is detected in the classpath.
 *
 * @author Sjoerd Talsma
 */
public final class SpanContextManager implements ContextManager<Span> {

    private final SpanManager spanManager; // TODO also make the SpanManager service-loadable.

    public SpanContextManager() {
        this(DefaultSpanManager.getInstance());
    }

    public SpanContextManager(SpanManager spanManager) {
        if (spanManager == null) throw new NullPointerException("SpanManager is <null>.");
        this.spanManager = spanManager;
    }

    @Override
    public Context<Span> initializeNewContext(Span value) {
        return new ManagedSpanContext(spanManager.manage(value));
    }

    @Override
    public Context<Span> getActiveContext() {
        return new ManagedSpanContext(spanManager.currentSpan());
    }

    @Override
    public String toString() {
        return "SpanContextManager{" + spanManager + '}';
    }

}
