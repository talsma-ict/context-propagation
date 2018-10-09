/*
 * Copyright 2016-2018 Talsma ICT
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
package nl.talsmasoftware.context.futures;

import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Snapshot holder that is used internally to temporarily 'hold' a context snapshot
 * to be propagated from one {@code CompletionStage} to another.
 *
 * @author Sjoerd Talsma
 */
final class ContextSnapshotHolder implements Consumer<ContextSnapshot>, Supplier<ContextSnapshot> {
    private volatile ContextSnapshot snapshot;

    /**
     * Create a new snapshot holder initially containing either the provided snapshot,
     * or takes a new snapshot as initial value.
     *
     * @param snapshot The snapshot to hold initially.
     *                 Optional, if {@code null} the holder will initialize with a new snapshot.
     */
    ContextSnapshotHolder(ContextSnapshot snapshot) {
        this.accept(snapshot == null ? ContextManagers.createContextSnapshot() : snapshot);
    }

    /**
     * Accept a new snapshot (i.e. after a single stage is completed) to be propagated
     * into another completion stage.
     *
     * @param snapshot The snapshot to hold (required, must not be {@code null})
     */
    @Override
    public void accept(ContextSnapshot snapshot) {
        this.snapshot = requireNonNull(snapshot, "Context snapshot is <null>.");
    }

    /**
     * Returns the current snapshot, normally to activate when beginning a new completion stage.
     *
     * @return The held snapshot (should never be {@code null})
     */
    @Override
    public ContextSnapshot get() {
        return snapshot;
    }

}
