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
import io.opentracing.ScopeManager;
import io.opentracing.util.GlobalTracer;
import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Sjoerd Talsma
 */
public class OpentracingScopeManager implements ContextManager<Scope> {

    @Override
    public Context<Scope> getActiveContext() {
        ScopeManager scopeManager = GlobalTracer.get().scopeManager();
        return new ScopeContext(scopeManager.active(), true);
    }

    @Override
    public Context<Scope> initializeNewContext(final Scope scope) {
        ScopeManager scopeManager = GlobalTracer.get().scopeManager();
        return new ScopeContext(scope == null ? null : scopeManager.activate(scope.span(), false), false);
    }

    private static class ScopeContext implements Context<Scope> {
        private final Scope scope;
        private final AtomicBoolean closed;

        private ScopeContext(Scope scope, boolean alreadyClosed) {
            this.scope = scope;
            this.closed = new AtomicBoolean(alreadyClosed);
        }

        @Override
        public Scope getValue() {
            return scope;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                scope.close();
            }
        }
    }

}
