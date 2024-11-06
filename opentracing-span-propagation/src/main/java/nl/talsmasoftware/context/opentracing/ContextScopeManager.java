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
package nl.talsmasoftware.context.opentracing;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.core.threadlocal.AbstractThreadLocalContext;

/**
 * Our own implementation of the opentracing {@linkplain ScopeManager}.
 *
 * <p>
 * Manages opentracing {@linkplain Scope} and allows it to be nested within another active scope,
 * taking care to restore the previous value when closing an active scope.
 *
 * <p>
 * This manager is based on our {@linkplain nl.talsmasoftware.context.core.threadlocal.AbstractThreadLocalContext}
 * implementation. Compared to the 'standard' {@linkplain io.opentracing.util.ThreadLocalScopeManager}
 * this implementation has the following advantages:
 * <ol>
 * <li>Close is explicitly idempotent; closing more than once has no additional side-effects
 * (even when finishOnClose is set to {@code true}).</li>
 * <li>More predictable behaviour for out-of-order closing of scopes.
 * Although this is explicitly unsupported by the opentracing specification,
 * we think having consistent and predictable behaviour is an advantage.
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
    @Override
    public Scope activate(Span span) {
        return new ThreadLocalSpanContext(span);
    }

    @Override
    public Span activeSpan() {
        return ThreadLocalSpanContext.currentSpan();
    }

    /**
     * Initializes a new context for the given {@linkplain Span}.
     *
     * @param value The span to activate.
     * @return The new active 'Scope'.
     * @see #activate(Span)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Context<Span> initializeNewContext(Span value) {
        return (Context<Span>) activate(value);
    }

    @Override
    public Span getActiveContextValue() {
        return activeSpan();
    }

    @Override
    public void clear() {
        ThreadLocalSpanContext.remove();
    }

    /**
     * @return String representation for this context manager.
     */
    public String toString() {
        return getClass().getSimpleName();
    }

    private static final class ThreadLocalSpanContext extends AbstractThreadLocalContext<Span> implements Scope {
        private static final ThreadLocal<ThreadLocalSpanContext> SPAN_CONTEXT =
                AbstractThreadLocalContext.threadLocalInstanceOf(ThreadLocalSpanContext.class);

        private ThreadLocalSpanContext(Span newValue) {
            super(newValue);
        }

        private static Span currentSpan() {
            ThreadLocalSpanContext current = SPAN_CONTEXT.get();
            return current != null ? current.getValue() : null;
        }

        private static void remove() {
            SPAN_CONTEXT.remove();
        }
    }
}
