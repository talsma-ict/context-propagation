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

import io.opentelemetry.context.ContextStorage;
import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.api.ContextSnapshot;

import java.util.function.Function;

import static io.opentelemetry.context.Context.root;
import static nl.talsmasoftware.context.managers.opentelemetry.OpenTelemetryContextStorageWrapper.CAPTURE_LOCK;

/**
 * Context Manager for the open telemetry {@linkplain io.opentelemetry.context.Context Context}.
 *
 * <p>
 * Includes the {@linkplain io.opentelemetry.context.Context#current() current}
 * open telemetry {@linkplain io.opentelemetry.context.Context Context}
 * in {@linkplain ContextSnapshot#capture() captured} {@linkplain ContextSnapshot}s.
 *
 * <p>
 * The ContextManager delegates {@linkplain java.lang.ThreadLocal ThreadLocal} management to the
 * default {@linkplain io.opentelemetry.context.Context OpenTelemetry Context} storage.
 * <ul>
 *     <li>Obtaining the current context value is delegated to
 *     {@linkplain io.opentelemetry.context.Context#current()}.
 *     <li>Intializing a new context value is delegated to
 *     {@linkplain io.opentelemetry.context.Context#makeCurrent()}.
 * </ul>
 *
 * <h2>Bridge function</h2>
 * <p>
 * Besides capturing the current Context, this module also {@linkplain ContextStorage#addWrapper(Function) adds}
 * an {@linkplain OpenTelemetryContextStorageWrapper} to the configured open telemetry {@linkplain ContextStorage}.<br>
 * This wrapper includes captured {@linkplain ContextSnapshot}s into each Context returned
 * from {@linkplain io.opentelemetry.context.Context#current()},
 * thereby bridging <em>all</em> supported {@linkplain ContextManager} implementations over the
 * open telemetry {@linkplain io.opentelemetry.context.Context Context} mechanism.
 *
 * <p>
 * There is no need to instantiate the context manager yourself.<br>
 * Including it on the classpath will:
 * <ol>
 *     <li>allow the API to automatically include it in captured
 *     {@linkplain nl.talsmasoftware.context.api.ContextSnapshot ContextSnapshot}s.
 *     <li>automatically add an {@linkplain OpenTelemetryContextStorageWrapper},
 *     bridging ContextSnapshots to be propagated <em>over</em> open telemetry context management.
 * </ol>
 *
 * @implNote The method {@linkplain #clear()} is a no-op; it will <strong>not</strong> clear the current context
 * as we are not in control over the storage ourselves.
 * This does make it very important that all reactivated context snapshots are properly closed again.
 */
public class OpenTelemetryContextManager implements ContextManager<io.opentelemetry.context.Context> {
    static {
        // Register the otel storage wrapper directly when the context manager class is loaded.
        // This should be during application initialization.
        ContextStorage.addWrapper(OpenTelemetryContextStorageWrapper::new);
    }

    @SuppressWarnings("java:S1874") // This is the only place where the deprecated constructor should be used.
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
        // no-op default constructor declared explicitly for deprecation.
    }

    /**
     * The active context value, obtained from the default opentelemetry context storage.
     *
     * @return The active context value.
     * @see io.opentelemetry.context.Context#current()
     */
    @Override
    public io.opentelemetry.context.Context getActiveContextValue() {
        if (CAPTURE_LOCK.get() == null) {
            try {
                CAPTURE_LOCK.set(this); // prevent ContextSnapshot in Context in ContextSnapshot by recursion.
                return io.opentelemetry.context.Context.current();
            } finally {
                CAPTURE_LOCK.remove();
            }
        } else {
            return null;
        }
    }

    /**
     * Activate the specified opentelemetry context.
     *
     * @param value The value to make the current opentelemetry context.
     * @return A context that will close the scope created by opentelemetry.
     * @see io.opentelemetry.context.Context#makeCurrent()
     */
    @Override
    public Context activate(final io.opentelemetry.context.Context value) {
        return (value == null ? root() : value).makeCurrent()::close;
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
        // no-op
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
