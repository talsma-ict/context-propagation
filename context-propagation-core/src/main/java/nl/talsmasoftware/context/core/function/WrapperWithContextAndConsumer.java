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
package nl.talsmasoftware.context.core.function;

import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.core.delegation.WrapperWithContext;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@linkplain WrapperWithContext} that takes an additional {@link Consumer} accepting a resulting snapshot,
 * to be captured at the end of the function.
 *
 * <p>
 * This consumer is optional.<br>
 * All subclasses <strong>must</strong> avoid capturing unnecessary snapshots if the snapshot consumer is {@code null}.
 *
 * @param <T> The type of the wrapped delegate object.
 */
abstract class WrapperWithContextAndConsumer<T> extends WrapperWithContext<T> {
    /**
     * Logger for the concrete subclass.
     */
    protected final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * The context snapshot consumer to provide a new snapshot to after the function is complete.
     *
     * <p>
     * This consumer may be {@code null} in which case <strong>no new snapshot</strong> must be captured.
     */
    protected final Consumer<ContextSnapshot> contextSnapshotConsumer;

    /**
     * A new wrapper around a delegate object with a {@linkplain ContextSnapshot}.
     *
     * @param snapshot                The context snapshot to be reactivated around the delegate (required).
     * @param delegate                The delegate action to perform.
     * @param contextSnapshotConsumer An optional post-action consumer to receive a new context snapshot taken after the action.
     */
    protected WrapperWithContextAndConsumer(ContextSnapshot snapshot, T delegate, Consumer<ContextSnapshot> contextSnapshotConsumer) {
        super(snapshot, delegate);
        this.contextSnapshotConsumer = contextSnapshotConsumer;
    }

    /**
     * A new wrapper around a delegate object with a {@linkplain ContextSnapshot}.
     *
     * @param contextSnapshotSupplier Supplies the context snapshot to be reactivated around the delegate (required).
     * @param delegate                The delegate action to perform.
     * @param contextSnapshotConsumer An optional post-action consumer to receive a new context snapshot taken after the action.
     */
    protected WrapperWithContextAndConsumer(Supplier<ContextSnapshot> contextSnapshotSupplier, T delegate, Consumer<ContextSnapshot> contextSnapshotConsumer) {
        super(contextSnapshotSupplier, delegate);
        this.contextSnapshotConsumer = contextSnapshotConsumer;
    }

    /**
     * Capture a new {@linkplain ContextSnapshot} and pass it to the {@code contextSnapshotConsumer}.
     *
     * <p>
     * If there is no {@code contextSnapshotConsumer},
     * no unnecessary snapshot will be {@linkplain ContextSnapshot#capture() captured}.
     */
    protected void captureResultSnapshotIfRequired() {
        if (contextSnapshotConsumer != null) {
            ContextSnapshot resultSnapshot = ContextSnapshot.capture();
            logger.log(Level.FINEST, "Captured context snapshot after delegation: {0}", resultSnapshot);
            contextSnapshotConsumer.accept(resultSnapshot);
        }
    }
}
