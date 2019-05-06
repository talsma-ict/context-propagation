/*
 * Copyright 2016-2019 Talsma ICT
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
package nl.talsmasoftware.context.opentracing;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManager;
import nl.talsmasoftware.context.threadlocal.AbstractThreadLocalContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Our own implementation of the opentracing {@linkplain ScopeManager}.
 *
 * <p>
 * Manages opentracing {@linkplain Scope} and allows it to be nested within another active scope,
 * taking care to restore the previous value when closing an active scope.
 *
 * <p>
 * This manager is based on our {@linkplain nl.talsmasoftware.context.threadlocal.AbstractThreadLocalContext}
 * implementation. Compared to the 'standard' {@linkplain io.opentracing.util.ThreadLocalScopeManager}
 * this implementation has the following advantages:
 * <ol>
 * <li>Close is explicitly idempotent; closing more than once has no additional side-effects
 * (even when finishOnClose is set to {@code true}).</li>
 * <li>More predictable behaviour for out-of-order closing of scopes.
 * Although this is explicitly unsupported by the opentracing specification,
 * we think having consistent and predictable behaviour is an advantage.
 * <li>Support for {@link nl.talsmasoftware.context.observer.ContextObserver}.
 * See https://github.com/opentracing/opentracing-java/issues/334 explicitly wanting this.
 * </ol>
 *
 * <p>
 * Please note that this scope manager is not somehow automatically enabled.
 * You will have to provide an instance to your tracer of choice when initializing it.
 *
 * <p>
 * The <em>active span</em> that is automatically propagated when using this
 * {@code opentracing-span-propagation} library in combination with
 * the context aware support classes is from the registered ScopeManager
 * from the {@linkplain io.opentracing.util.GlobalTracer}.
 *
 * @since 1.0.6
 */
public class ContextScopeManager implements ScopeManager, ContextManager<Span> {
    /**
     * Makes the given span the new active span.
     *
     * @param span              The span to become the active span.
     * @param finishSpanOnClose Whether the span should automatically finish when closing the resulting scope.
     * @return The new active scope (must be closed from the same thread).
     */
    @Override
    public Scope activate(Span span, boolean finishSpanOnClose) {
        return new ThreadLocalSpanContext(getClass(), span, finishSpanOnClose);
    }

    @Override
    public Scope activate(Span span) {
        return new ThreadLocalSpanContext(getClass(), span, false);
    }

    /**
     * The currently active {@link Scope} containing the {@linkplain #activeSpan() active span}.
     *
     * @return the active scope, or {@code null} if none could be found.
     */
    @Override
    public Scope active() {
        return ThreadLocalSpanContext.current();
    }

    @Override
    public Span activeSpan() {
        Context<Span> current = ThreadLocalSpanContext.current();
        return current == null ? null : current.getValue();
    }

    /**
     * Initializes a new context for the given {@linkplain Span}.
     *
     * @param value The span to activate.
     * @return The new active 'Scope'.
     * @see #activate(Span, boolean)
     */
    @Override
    public Context<Span> initializeNewContext(Span value) {
        return new ThreadLocalSpanContext(getClass(), value, false);
    }

    /**
     * @return The active span context (this is identical to the active scope).
     * @see #active()
     */
    @Override
    public Context<Span> getActiveContext() {
        return ThreadLocalSpanContext.current();
    }

    /**
     * @return String representation for this context manager.
     */
    public String toString() {
        return getClass().getSimpleName();
    }

    private static final class ThreadLocalSpanContext extends AbstractThreadLocalContext<Span> implements Scope {
        private final AtomicBoolean finishOnClose;

        private ThreadLocalSpanContext(Class<? extends ContextManager<? super Span>> contextManagerType, Span newValue, boolean finishOnClose) {
            super(contextManagerType, newValue);
            this.finishOnClose = new AtomicBoolean(finishOnClose);
        }

        private static ThreadLocalSpanContext current() {
            return current(ThreadLocalSpanContext.class);
        }

        @Override
        public Span span() {
            return value;
        }

        @Override
        public void close() {
            super.close();
            if (finishOnClose.compareAndSet(true, false) && value != null) {
                value.finish();
            }
        }
    }
}
