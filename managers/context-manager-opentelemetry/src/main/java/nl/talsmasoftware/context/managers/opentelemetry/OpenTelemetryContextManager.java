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

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;

public class OpenTelemetryContextManager implements ContextManager<io.opentelemetry.context.Context> {
    private static final OpenTelemetryContextManager INSTANCE = new OpenTelemetryContextManager();

    /**
     * Returns the singleton instance of the {@code OpenTelemetryContextManager}.
     * <p>
     * The ServiceLoader supports a static {@code provider()} method to resolve services since Java 9.
     *
     * @return The OpenTelemetry context manager.
     */
    public static OpenTelemetryContextManager provider() {
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
    public OpenTelemetryContextManager() {
    }

    @Override
    public io.opentelemetry.context.Context getActiveContextValue() {
        return io.opentelemetry.context.Context.current();
    }

    @Override
    public Context<io.opentelemetry.context.Context> initializeNewContext(final io.opentelemetry.context.Context value) {
        return new ScopeWrappingContext<io.opentelemetry.context.Context>(value.makeCurrent()) {
            @Override
            public io.opentelemetry.context.Context getValue() {
                return value;
            }
        };
    }

    @Override
    public void clear() {
    }

}
