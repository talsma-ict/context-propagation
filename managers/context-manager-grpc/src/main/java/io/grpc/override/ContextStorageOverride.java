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

public class ContextStorageOverride extends Context.Storage {
    @Override
    public Context doAttach(Context toAttach) {
        return GrpcContextManager.provider().doAttach(toAttach);
    }

    @Override
    public void detach(Context toDetach, Context toRestore) {
        GrpcContextManager.provider().detach(toDetach, toRestore);
    }

    @Override
    public Context current() {
        return GrpcContextManager.provider().current();
    }
}
