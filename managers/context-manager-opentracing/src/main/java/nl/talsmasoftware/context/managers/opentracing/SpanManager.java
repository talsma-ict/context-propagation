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
package nl.talsmasoftware.context.managers.opentracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager for <a href="http://opentracing.io/">OpenTracing</a> {@linkplain Span}.
 *
 * <p>
 * Management of {@linkplain Span spans} is delegated to the {@linkplain GlobalTracer}.
 *
 * @author Sjoerd Talsma
 */
public class SpanManager implements ContextManager<Span> {
    /**
     * Singleton instance of this class.
     */
    private static final SpanManager INSTANCE = new SpanManager();

    /**
     * Returns the singleton instance of the {@code SpanManager}.
     *
     * <p>
     * The ServiceLoader supports a static {@code provider()} method to resolve services since Java 9.
     *
     * @return The OpenTracing Span context manager.
     */
    public static SpanManager provider() {
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
    public SpanManager() {
    }

    /**
     * Return the {@link GlobalTracer#activeSpan() active span}.
     *
     * @return The currently active span as a context.
     */
    @Override
    public Span getActiveContextValue() {
        return activeSpan();
    }

    @Override
    public void clear() {
        // Opentracing API does not support clearing the span.
    }

    /**
     * {@linkplain io.opentracing.ScopeManager#activate(Span) Activates} the given {@linkplain Span span}.
     * <p>
     * {@linkplain Context#close() Closing} the returned {@link Context} will also close the
     * corresponding {@link Scope} as it was also activated by us.<br>
     * As a result of the opentracing 'rules' for scopes, <strong>every</strong> initialized {@link Context}
     * <strong>must be closed</strong>.<br>
     * The span will <strong>not</strong> be automatically {@link Span#finish() finished}
     * when the context is closed; this {@linkplain ContextManager} just propagates the Span
     * and does not concern itself with the Span's lifecycle.
     * <p>
     * It is safe to close the resulting context more than once.
     * <p>
     * No scope is activated if the specified {@linkplain Span} is {@code null}.
     *
     * @param span The span to make the active span of the current OpenTracing scope.
     * @return The new context that <strong>must</strong> be closed.
     */
    @Override
    public Context<Span> initializeNewContext(final Span span) {
        return new SpanContext(span);
    }

    /**
     * @return Simple class name as this class carries no internal state.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private static Span activeSpan() {
        return GlobalTracer.get().activeSpan();
    }

    private static class SpanContext implements Context<Span> {
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final Scope scope;

        private SpanContext(Span span) {
            this.scope = span != null ? GlobalTracer.get().activateSpan(span) : null;
        }

        @Override
        public Span getValue() {
            return closed.get() ? null : activeSpan();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true) && scope != null) {
                scope.close();
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + (closed.get() ? "{closed}" : "{" + getValue() + '}');
        }
    }

}
