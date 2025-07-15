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

public class OpenTelemetryContextStorageWrapper implements ContextStorage {

    private static final ContextKey<ContextSnapshot> OTEL_SNAPSHOT_KEY = ContextKey.named("contextsnapshot-over-otel");
    static final ThreadLocal<Boolean> CAPTURE = ThreadLocal.withInitial(() -> Boolean.TRUE);

    private final ContextStorage delegate;

    public OpenTelemetryContextStorageWrapper(ContextStorage delegate) {
        this.delegate = requireNonNull(delegate, "Delegate ContextStorage is <null>.");
    }

    @Override
    public Context root() {
        return delegate.root();
    }

    @Override
    public Context current() {
        Context current = delegate.current();
        if (current == null) {
            current = root();
        }

        ContextSnapshot snapshot = null;
        if (Boolean.TRUE.equals(CAPTURE.get())) {
            try {
                CAPTURE.set(false); // prevent otel-context in snapshot in otel-context
                snapshot = ContextSnapshot.capture();
            } finally {
                CAPTURE.set(true);
            }
        }
        return current.with(OTEL_SNAPSHOT_KEY, snapshot);
    }

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
