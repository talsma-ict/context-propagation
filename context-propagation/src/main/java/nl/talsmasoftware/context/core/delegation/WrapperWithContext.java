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
package nl.talsmasoftware.context.core.delegation;

import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.delegation.ContextSnapshotSupplier;

/**
 * Wrapper that contains a context snapshot.
 *
 * @author Sjoerd Talsma
 */
public abstract class WrapperWithContext<T> extends nl.talsmasoftware.context.delegation.Wrapper<T> {

    private final ContextSnapshotSupplier supplier;
    private volatile ContextSnapshot snapshot;

    /**
     * Creates a new Wrapper with the specified context snapshot.
     *
     * @param snapshot The context snapshot (required, non-{@code null})
     * @param delegate The wrapped delegate object providing core functionality
     */
    protected WrapperWithContext(final ContextSnapshot snapshot, final T delegate) {
        super(delegate);
        this.supplier = null;
        this.snapshot = snapshot;
        if (snapshot == null) {
            throw new NullPointerException("No context snapshot provided to " + this + '.');
        }
    }

    /**
     * Wraps the delegate and provides a context snapshot.
     * <p>
     * <strong>Note:</strong> <em>Make sure the supplier function does <strong>not</strong> obtain the context snapshot
     * from any threadlocal storage! The wrapper is designed to propagate contexts from one thread to another.
     * Therefore, the snapshot must be {@link nl.talsmasoftware.context.ContextManagers#createContextSnapshot() captured}
     * in the source thread and {@link ContextSnapshot#reactivate() reactivated} in the target thread.
     * If unsure, please use the
     * {@link #WrapperWithContext(ContextSnapshot, Object) constructor with snapshot} instead.</em>
     *
     * @param supplier The supplier for the (fixed!) context snapshot.
     *                 This can be a straightforward 'holder' object or an ongoing background call.
     *                 Please do <strong>not</strong> make this supplier function access any {@code ThreadLocal} value,
     *                 as the wrapper is designed to propagate the snapshot from thread to thread!
     * @param delegate The delegate object to be wrapped.
     * @see #WrapperWithContext(ContextSnapshot, Object)
     */
    protected WrapperWithContext(ContextSnapshotSupplier supplier, T delegate) {
        super(delegate);
        this.supplier = supplier;
        this.snapshot = null;
        if (supplier == null) {
            throw new NullPointerException("No context snapshot supplier provided to " + this + '.');
        }
    }

    /**
     * Calls the supplier for the context snapshot, making sure it is called only once for this wrapper instance.
     *
     * @return The snapshot value.
     */
    protected ContextSnapshot snapshot() {
        // Note: Double-checked locking works fine in recent JDKs
        // Discussion here: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590407
        if (snapshot == null && supplier != null) synchronized (this) {
            if (snapshot == null) snapshot = supplier.get();
            if (snapshot == null) throw new NullPointerException("Context snapshot is <null>.");
        }
        return snapshot;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + snapshot().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (super.equals(other) && snapshot().equals(((WrapperWithContext<?>) other).snapshot()));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{delegate=" + delegate() + (snapshot == null ? "" : ", " + snapshot) + '}';
    }

}
