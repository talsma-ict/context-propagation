/*
 * Copyright 2016-2025 Talsma ICT
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

/**
 * Context Manager that delegates {@linkplain java.lang.ThreadLocal ThreadLocal} management to the
 * default {@linkplain io.opentelemetry.context.Context OpenTelemetry Context} storage.
 *
 * <p>
 * <dl>
 *     <dt><strong>{@linkplain #getActiveContextValue()}</strong></dt>
 *     <dd>Delegated to {@linkplain io.opentelemetry.context.Context#current()}</dd>
 *     <dt><strong>{@linkplain #initializeNewContext(io.opentelemetry.context.Context)}</strong></dt>
 *     <dd>Delegated to {@linkplain io.opentelemetry.context.Context#makeCurrent()}</dd>
 *     <dt><strong>{@linkplain #clear()}</strong></dt>
 *     <dd>no-op: Does <strong>not</strong> clear the current context as we are not managing it ourselves.
 *     This does make it very important that all initialized contexts are properly closed again.</dd>
 * </dl>
 *
 * <p>
 * There is no need to instantiate the context manager yourself.
 * Including it on the classpath will allow the API to automatically include it in
 * {@linkplain nl.talsmasoftware.context.api.ContextSnapshot context snapshots}.
 */
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

    /**
     * The active context value, obtained from the default opentelemetry context storage.
     *
     * @return The active context value.
     * @see io.opentelemetry.context.Context#current()
     */
    @Override
    public io.opentelemetry.context.Context getActiveContextValue() {
        return io.opentelemetry.context.Context.current();
    }

    /**
     * Activate the specified opentelemetry context.
     *
     * @param value The value to make the current opentelemetry context.
     * @return A context that will close the scope created by opentelemetry.
     * @see io.opentelemetry.context.Context#makeCurrent()
     */
    @Override
    public Context initializeNewContext(final io.opentelemetry.context.Context value) {
        return new ScopeWrappingContext(value.makeCurrent());
    }

    /**
     * No-op.
     *
     * <p>
     * This method performs no actions. The current OpenTelemetry Context is <strong>not</strong> closed.
     * Also, no exceptions are thrown from this method.
     */
    @Override
    public void clear() {
    }

    /**
     * Representative toString for this manager.
     *
     * @return simple class name, as this manager contains no state itself.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
