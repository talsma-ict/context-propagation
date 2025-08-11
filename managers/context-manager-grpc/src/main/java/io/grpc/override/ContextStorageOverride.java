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
package io.grpc.override;

import io.grpc.Context;
import nl.talsmasoftware.context.managers.grpc.GrpcContextManager;

/**
 * Overrides the default {@link Context.Storage} implementation with the {@link GrpcContextManager}.
 *
 * @author Sjoerd Talsma
 * @since 2.0.0
 */
public class ContextStorageOverride extends Context.Storage {
    /**
     * Create a new context storage override that delegates to the {@link GrpcContextManager}.
     */
    public ContextStorageOverride() {
        super();
    }

    /**
     * Attach the gRPC context to the current thread using the {@link GrpcContextManager}.
     *
     * @param toAttach the context to be attached
     * @return A Context that should be passed back into {@code detach(Context, Context)}
     * as the {@code toRestore} parameter.
     * @see GrpcContextManager#doAttach(Context)
     */
    @Override
    public Context doAttach(Context toAttach) {
        return GrpcContextManager.provider().doAttach(toAttach);
    }

    /**
     * Detach the gRPC context from the current thread using the {@link GrpcContextManager}.
     *
     * @param toDetach  the context to be detached.
     * @param toRestore the context to be restored.
     * @see GrpcContextManager#detach(Context, Context)
     */
    @Override
    public void detach(Context toDetach, Context toRestore) {
        GrpcContextManager.provider().detach(toDetach, toRestore);
    }

    /**
     * Returns the current gRPC context from the {@link GrpcContextManager}.
     *
     * @return The current gRPC context.
     * @see GrpcContextManager#current()
     */
    @Override
    public Context current() {
        return GrpcContextManager.provider().current();
    }
}
