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

    private static ContextSnapshot.Reactivation reactivateSnapshot(Context context) {
        ContextSnapshot snapshot = context.get(OTEL_SNAPSHOT_KEY);
        return snapshot == null ? null : snapshot.reactivate();
    }
}
