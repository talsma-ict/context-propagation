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
package nl.talsmasoftware.context.managers.grpc;

import io.grpc.Context.Key;
import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.api.ContextSnapshot.Reactivation;

import java.util.logging.Logger;

import static io.grpc.Context.ROOT;
import static io.grpc.Context.key;

/**
 * {@link ContextManager} for gRPC {@link io.grpc.Context}.
 *
 * <p>
 * Besides including the gRPC context in captured {@link ContextSnapshot}s,
 * this manager also provides a gRPC {@link io.grpc.Context.Storage} override acting as a bridge.
 * This means that current {@link ContextSnapshot}s are included in gRPC {@link io.grpc.Context}s as well.
 *
 * @author Sjoerd Talsma
 * @since 2.0.0
 */
public class GrpcContextManager extends io.grpc.Context.Storage implements ContextManager<io.grpc.Context> {
    private static final GrpcContextManager INSTANCE = new GrpcContextManager();
    private static final Logger LOGGER = Logger.getLogger(GrpcContextManager.class.getName());
    private static final Key<ContextSnapshot> GRPC_SNAPSHOT_KEY = key("contextsnapshot-over-grpc");
    private static final Key<Reactivation> GRPC_REACTIVATION_KEY = key("contextsnapshot-reactivation");
    private static final ThreadLocal<String> CAPTURING = new ThreadLocal<>();
    private static final io.grpc.Context DUMMY = io.grpc.Context.ROOT.withValue(key("dummy"), "dummy");
    private static final ThreadLocal<io.grpc.Context> STORAGE = new ThreadLocal<>();

    /**
     * Default constructor for java8 ServiceLoader.
     */
    public GrpcContextManager() {
        // legacy constructor for java8 ServiceLoader.
    }

    /**
     * Static {@code provider()} method for ServiceLoader since Java 9.
     *
     * @return The gRPC context manager.
     */
    public static GrpcContextManager provider() {
        return INSTANCE;
    }

    /**
     * The current gRPC context.
     *
     * <p>
     * This is the current gRPC context which <strong>does</strong> contain a {@link ContextSnapshot}.
     * This method is accessed by {@link io.grpc.Context#current()}
     * to include the {@link ContextSnapshot} in new gRPC contexts.
     *
     * @return The current gRPC context with a {@link ContextSnapshot} attached.
     * @see #current()
     */
    @Override
    public io.grpc.Context current() {
        io.grpc.Context current = ROOT;
        if (CAPTURING.get() == null) {
            try {
                CAPTURING.set("GRPC");
                current = nullToRoot(STORAGE.get()).withValue(GRPC_SNAPSHOT_KEY, ContextSnapshot.capture());
            } finally {
                CAPTURING.remove();
            }
        }
        return current;
    }

    /**
     * Attach the given gRPC context to the current thread.
     *
     * <p>
     * This makes the given gRPC context the current gRPC context.
     * If the gRPC context contains a {@link ContextSnapshot}, it will be reactivated.
     * The previous gRPC context will be returned as the {@code toRestore} context.
     * It <em>must</em> be restored in the same thread, which then closes the reactivated ContextSnapshot.
     *
     * @param toAttach the context to be attached
     * @return The previous gRPC context to be restored in the corresponding detach call.
     * @see #activate(io.grpc.Context)
     * @see #detach(io.grpc.Context, io.grpc.Context)
     */
    @Override
    public io.grpc.Context doAttach(io.grpc.Context toAttach) {
        io.grpc.Context toRestore = nullToRoot(STORAGE.get());
        ContextSnapshot snapshot = toAttach == null ? null : GRPC_SNAPSHOT_KEY.get(toAttach);
        if (snapshot != null) {
            toAttach = toAttach.withValue(GRPC_SNAPSHOT_KEY, null);
            toRestore = toRestore.withValue(GRPC_REACTIVATION_KEY, snapshot.reactivate());
        }
        STORAGE.set(rootToNull(toAttach));
        return toRestore;
    }

    /**
     * Detach the given gRPC context from the current thread, restoring the previous gRPC context.
     *
     * <p>
     * The previous gRPC context to be restored should contain a snapshot reactivation which will be closed,
     * thereby <em>also</em> restoring the previous context snapshot.
     *
     * @param toDetach  the context to be detached. Should be, or be equivalent to, the current
     *                  context of the current scope.
     * @param toRestore the context to be the current. Should be, or be equivalent to, the context
     *                  of the outer scope.
     */
    @Override
    public void detach(io.grpc.Context toDetach, io.grpc.Context toRestore) {
        Reactivation reactivation = toRestore == null ? null : GRPC_REACTIVATION_KEY.get(toRestore);
        if (reactivation != null) {
            reactivation.close();
            toRestore = toRestore.withValue(GRPC_REACTIVATION_KEY, null);
        }
        STORAGE.set(rootToNull(toRestore));
    }

    /**
     * Clear the current gRPC context.
     */
    @Override
    public void clear() {
        STORAGE.remove();
    }

    /**
     * The current gRPC context.
     *
     * <p>
     * This is the managed gRPC context, which does <em>not</em> contain a {@link ContextSnapshot}.
     * This method is accessed by the {@link ContextSnapshot} implementation itself to include the gRPC context in the
     * captured {@link ContextSnapshot}.
     *
     * @return The current gRPC context if available, or {@code null} otherwise.
     * @see #current()
     */
    @Override
    public io.grpc.Context getActiveContextValue() {
        if (CAPTURING.get() != null) {
            return DUMMY;
        }
        try {
            CAPTURING.set("SNAPSHOT");
            return STORAGE.get();
        } finally {
            CAPTURING.remove();
        }
    }

    /**
     * Activate the given gRPC context.
     *
     * <p>
     * This stores the given gRPC context as the current gRPC context. The previous gRPC context will be restored
     * when the returned {@link Context} is closed.
     *
     * @param value The gRPC context to activate.
     * @return A context that restores the previous gRPC context when closed.
     * @see #doAttach(io.grpc.Context)
     */
    @Override
    public Context activate(io.grpc.Context value) {
        if (value == DUMMY) {
            return () -> {
            };
        }
        final io.grpc.Context toRestore = doAttach(value);
        return () -> detach(value, toRestore);
    }

    private static io.grpc.Context nullToRoot(io.grpc.Context context) {
        return context == null ? ROOT : context;
    }

    private static io.grpc.Context rootToNull(io.grpc.Context context) {
        return context == ROOT ? null : context;
    }
}
