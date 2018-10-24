/*
 * Copyright 2016-2018 Talsma ICT
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
import io.opentracing.util.GlobalTracer;
import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager for <a href="http://opentracing.io/">OpenTracing</a> {@linkplain Span}.
 * <p>
 * Management of {@linkplain Span spans} is delegated to the {@linkplain ScopeManager}
 * from the {@linkplain GlobalTracer}.
 *
 * @author Sjoerd Talsma
 */
public class SpanManager implements ContextManager<Span> {

    /**
     * Return the {@link GlobalTracer#activeSpan() active span} as a {@link Context}.
     * <p>
     * Please note: Closing this context will <strong>not</strong> close the corresponding OpenTracing scope
     * as it is not ours to manage.
     *
     * @return The currently active span as a context.
     */
    @Override
    public Context<Span> getActiveContext() {
        Span activeSpan = GlobalTracer.get().activeSpan();
        return activeSpan == null ? null : new ManagedSpan(activeSpan, null);
    }

    /**
     * {@linkplain ScopeManager#activate(Span, boolean) activates} the given {@linkplain Span span}.
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
        Scope scope = span == null ? null : GlobalTracer.get().scopeManager().activate(span, false);
        return new ManagedSpan(span, scope);
    }

    private static class ManagedSpan implements Context<Span> {
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final Span span;
        private final Scope scope;

        private ManagedSpan(Span span, Scope scope) {
            this.span = span;
            this.scope = scope;
        }

        @Override
        public Span getValue() {
            return closed.get() ? null : span;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true) && scope != null) {
                scope.close();
            }
        }
    }

}
