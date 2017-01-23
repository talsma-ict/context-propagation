package nl.talsmasoftware.context.opentracing;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.SpanManager;
import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManager;

/**
 * {@link ContextManager} implementation for OpenTracing {@link Span} objects
 * delegating to known existing {@link SpanManager} implementations.
 * <p>
 * This adds {@code DefaultSpanManager} support to the
 * global {@link nl.talsmasoftware.context.ContextManagers#createContextSnapshot() context snapshot}.
 * <p>
 * This functionality is automatically added to the {@link nl.talsmasoftware.context.ContextManagers} class when
 * this JAR file (and hence the service definition) is detected on the classpath.
 *
 * @author Sjoerd Talsma
 */
public final class SpanContextManager implements ContextManager<Span> {

    @Override
    @SuppressWarnings("deprecation") // Intentional deprecation
    public Context<Span> initializeNewContext(Span value) {
        return new ManagedSpanContext(GlobalSpanManager.get().manage(value));
    }

    @Override
    @SuppressWarnings("deprecation") // Intentional deprecation
    public Context<Span> getActiveContext() {
        return new ManagedSpanContext(GlobalSpanManager.get().currentSpan());
    }

    @Override
    public String toString() {
        return "SpanContextManager";
    }

}
