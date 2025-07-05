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

import java.util.logging.Logger;

import static io.grpc.Context.ROOT;
import static io.grpc.Context.key;

public class GrpcContextManager extends io.grpc.Context.Storage implements ContextManager<io.grpc.Context> {
    private static final GrpcContextManager INSTANCE = new GrpcContextManager();
    private static final Key<ContextSnapshot> GRPC_SNAPSHOT_KEY = key("contextsnapshot-over-grpc");
    private static final Key<ContextSnapshot.Reactivation> GRPC_REACTIVATION_KEY = key("contextsnapshot-reactivation");
    private static final Logger LOGGER = Logger.getLogger(GrpcContextManager.class.getName());
    private static final ThreadLocal<io.grpc.Context> STORAGE = new ThreadLocal<io.grpc.Context>() {
        @Override
        public void set(io.grpc.Context value) {
            if (ROOT.equals(value)) { // prevent ROOT from being set
                value = null;
            }
            if (value != null) {
                // do not store ContextSnapshot or its reactivation in the threadlocal!
                if (GRPC_SNAPSHOT_KEY.get(value) != null) {
                    value = value.withValue(GRPC_SNAPSHOT_KEY, null);
                }
                if (GRPC_REACTIVATION_KEY.get(value) != null) {
                    value = value.withValue(GRPC_REACTIVATION_KEY, null);
                }
            }
            super.set(value);
        }
    };

    public GrpcContextManager() {
        // legacy constructor for java8 ServiceLoader.
    }

    public static GrpcContextManager provider() {
        return INSTANCE;
    }

    @Override
    public io.grpc.Context getActiveContextValue() {
        return STORAGE.get();
    }

    @Override
    public io.grpc.Context current() {
        io.grpc.Context current = getActiveContextValue();
        return (current == null ? ROOT : current).withValue(GRPC_SNAPSHOT_KEY, ContextSnapshot.capture());
    }

    @Override
    public Context activate(io.grpc.Context value) {
        io.grpc.Context previous = STORAGE.get();
        STORAGE.set(value);
        return () -> STORAGE.set(previous);
    }

    @Override
    public io.grpc.Context doAttach(io.grpc.Context toAttach) {
        io.grpc.Context toRestore = STORAGE.get();
        if (toRestore == null) {
            toRestore = ROOT;
        }
        STORAGE.set(toAttach);
        ContextSnapshot snapshot = toAttach == null ? null : GRPC_SNAPSHOT_KEY.get(toAttach);
        if (snapshot == null) {
            LOGGER.warning(() -> "No context snapshot found in gRPC context to be attached: " + toAttach);
            return toRestore;
        } else {
            return toRestore.withValue(GRPC_REACTIVATION_KEY, snapshot.reactivate());
        }
    }

    @Override
    public void detach(io.grpc.Context toDetach, io.grpc.Context toRestore) {
        ContextSnapshot.Reactivation reactivation = GRPC_REACTIVATION_KEY.get(toRestore);
        if (reactivation == null) {
            // should this ever happen?
            LOGGER.warning(() -> "No snapshot reactivation present in gRPC context to be restored: " + toRestore);
        } else {
            reactivation.close();
        }
        STORAGE.set(toRestore);
    }

    @Override
    public void clear() {
        STORAGE.remove();
    }

}
