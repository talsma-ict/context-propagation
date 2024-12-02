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
package nl.talsmasoftware.context.managers.opentelemetry;

import io.opentelemetry.context.Scope;
import nl.talsmasoftware.context.api.Context;

/**
 * Context wrapping an {@linkplain Scope OpenTelemetry Scope}, closing it when the context is closed.
 *
 * @param <T> The type of the contained value, left for the subclass to provide.
 */
class ScopeWrappingContext<T> implements Context<T> {
    private final Scope scope;

    /**
     * Creates a new context responsible for closing the provided {@linkplain Scope}.
     *
     * @param scope the opentelemetry scope to wrap.
     */
    ScopeWrappingContext(Scope scope) {
        this.scope = scope;
    }

    /**
     * Closes the wrapped {@linkplain Scope OpenTelemetry Scope}.
     *
     * @see Scope#close()
     */
    public void close() {
        scope.close();
    }
}
