/*
 * Copyright 2016-2024 Talsma ICT
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
package nl.talsmasoftware.context.functions;

import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.delegation.WrapperWithContext;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Package-convenience subclass for {@linkplain WrapperWithContext} that takes Java 8 generic functional interfaces
 * {@link Supplier} and {@link Consumer} instead of the specific Java 5 versions.
 *
 * @param <T> The type of the wrapped delegate object.
 * @deprecated Moved to package {@code nl.talsmasoftware.context.core.function}.
 */
@Deprecated
abstract class WrapperWithContextAndConsumer<T> extends WrapperWithContext<T> {

    /**
     * The context snapshot consumer to provide a new snapshot to after the function is completed, may be {@code null}.
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
        super(contextSnapshotSupplier == null ? null : contextSnapshotSupplier::get, delegate);
        this.contextSnapshotConsumer = contextSnapshotConsumer;
    }

    /**
     * @return An optional post-action consumer to receive a new context snapshot taken after the action.
     * @deprecated The functional wrappers currently no longer use this method.
     */
    @Deprecated
    protected Optional<Consumer<ContextSnapshot>> consumer() {
        return Optional.ofNullable(contextSnapshotConsumer);
    }

}
