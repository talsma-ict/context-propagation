/**
 * Copyright 2016-2017 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
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
    @SuppressWarnings("deprecation") // Intentional deprecation of GlobalSpanManager
    public Context<Span> initializeNewContext(Span value) {
        return new ManagedSpanContext(GlobalSpanManager.get().activate(value));
    }

    @Override
    @SuppressWarnings("deprecation") // Intentional deprecation of GlobalSpanManager
    public Context<Span> getActiveContext() {
        return new ManagedSpanContext(GlobalSpanManager.get().current().getSpan());
    }

    @Override
    public String toString() {
        return "SpanContextManager";
    }

}
