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

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import nl.talsmasoftware.context.api.ContextSnapshot;

import static java.util.Objects.requireNonNull;

/**
 * OpenTelemetry ContextStorage Wrapper.
 *
 * <p>
 * This wrapper provides functionality <em>around</em> a {@code delegate} ContextStorage.
 * <ol>
 *     <li>{@linkplain #current()} will {@linkplain ContextSnapshot#capture() capture} a new ContextSnapshot
 *     and add it to the returned open telemetry context.
 *     <li>{@linkplain #attach(Context)} will check if the provided open telemetry context contains a ContextSnapshot.
 *     It will {@linkplain ContextSnapshot#reactivate() reactivate} this snapshot while attaching the context.
 *     A {@linkplain Scope} is returned that will close both the attached context and the snapshot reactivation.
 *     <li>To prevent nested <em>ContextSnapshot</em> in <em>Context</em> in <em>ContextSnapshot</em> cycles,
 *     measures are taken that the captured snapshot will <em>not</em> include a current Context,
 *     when called from the {@linkplain #current()} method.
 * </ol>
 */
public class OpenTelemetryContextStorageWrapper implements ContextStorage {

    /**
     * Lock shared by {@linkplain #current()}
     * and {@linkplain OpenTelemetryContextManager#getActiveContextValue() getActiveContextValue()} methods.
     *
     * <p>
     * This prevents recursive inclusion of ContextSnapshot in open telemetry Context and vice versa,
     * while still allowing ContextSnapshots to be included in open telemetry Context and the other way around.
     */
    static final ThreadLocal<Object> CAPTURE_LOCK = new ThreadLocal<>();
    /**
     * Key for the ContextSnapshot included in an open telemetry Context.
     */
    private static final ContextKey<ContextSnapshot> OTEL_SNAPSHOT_KEY = ContextKey.named("contextsnapshot-over-otel");

    private final ContextStorage delegate;

    /**
     * Constructor for a new ContextStorageWrapper around the specified {@code delegate}.
     *
     * @param delegate The ContextStorage being wrapped (required, non-{@code null}).
     */
    public OpenTelemetryContextStorageWrapper(ContextStorage delegate) {
        this.delegate = requireNonNull(delegate, "Delegate ContextStorage is <null>.");
    }

    /**
     * The root context, returned unchanged from the wrapped delegate context storage.
     *
     * @return The root context.
     */
    @Override
    public Context root() {
        return delegate.root();
    }

    /**
     * The current context, including a ContextSnapshot that can be reactivated.
     *
     * @return The current context which includes a captured ContextSnapshot to be reactivated.
     * @implNote When this method is called from {@code ContextSnapshot.capture()},
     * it will simply return the current delegate context <em>without</em> a new ContextSnapshot.
     * This prevents endless recursion and nested contexts.
     */
    @Override
    public Context current() {
        Context current = delegate.current();
        if (current == null) {
            current = root();
        }

        ContextSnapshot snapshot = null;
        if (CAPTURE_LOCK.get() == null) {
            try {
                CAPTURE_LOCK.set(this); // prevent Context in ContextSnapshot in Context by recursion.
                snapshot = ContextSnapshot.capture();
            } finally {
                CAPTURE_LOCK.remove();
            }
        }
        return current.with(OTEL_SNAPSHOT_KEY, snapshot);
    }

    /**
     * Attaches the specified Context to the current thread, reactivating a contained ContextSnapshot.
     *
     * <p>
     * Closing the returned Scope also closes the snapshot reactivation.
     *
     * @param toAttach The context to be attached to the current thread.
     * @return A scope removes the context again when it is closed.
     */
    @Override
    public Scope attach(Context toAttach) {
        final ContextSnapshot snapshot = toAttach == null ? null : toAttach.get(OTEL_SNAPSHOT_KEY);
        if (snapshot == null) {
            return delegate.attach(toAttach);
        }

        final ContextSnapshot.Reactivation reactivation = snapshot.reactivate();
        final Scope scope = delegate.attach(toAttach);
        return () -> {
            try {
                scope.close();
            } finally {
                reactivation.close();
            }
        };
    }

}
