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
package nl.talsmasoftware.context.opentracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Sjoerd Talsma
 */
public class ScopeContextManager implements ContextManager<Scope> {

    @Override
    public Context<Scope> getActiveContext() {
        return new ScopeContext(GlobalTracer.get().scopeManager().active()) {
            @Override
            public void close() {
                // Don't auto-close pre-existing scope instances ?
            }
        };
    }

    @Override
    public Context<Scope> initializeNewContext(final Scope scope) {
        final Span activeSpan = scope == null ? null : scope.span();
        return new ScopeContext(GlobalTracer.get().scopeManager().activate(activeSpan, false));
    }

    private static class ScopeContext implements Context<Scope> {
        private final WrappedScope scope;

        private ScopeContext(Scope existingScope) {
            this.scope = existingScope == null || existingScope instanceof WrappedScope
                    ? (WrappedScope) existingScope
                    : new WrappedScope(existingScope);
        }

        @Override
        public Scope getValue() {
            return scope;
        }

        @Override
        public void close() {
            if (scope != null) scope.close();
        }
    }

    /**
     * Wrapper around {@link Scope} to make sure it is closed (by us) only once.
     */
    private static final class WrappedScope implements Scope {
        private final Scope delegate;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private WrappedScope(Scope wrapped) {
            this.delegate = wrapped;
        }

        @Override
        public void close() {
            if (delegate != null && closed.compareAndSet(false, true)) delegate.close();
        }

        @Override
        public Span span() {
            return delegate == null ? null : delegate.span();
        }
    }

}
