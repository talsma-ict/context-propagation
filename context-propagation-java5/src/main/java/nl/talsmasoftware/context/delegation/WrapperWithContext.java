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
package nl.talsmasoftware.context.delegation;

import nl.talsmasoftware.context.ContextSnapshot;

/**
 * Wrapper that also contains a fixed context snapshot.
 *
 * @author Sjoerd Talsma
 */
public abstract class WrapperWithContext<T> extends Wrapper<T> {

    private final ContextSnapshotSupplier supplier;
    private volatile ContextSnapshot _snapshot = null;
    protected final ContextSnapshotConsumer consumer;

    @Deprecated
    protected WrapperWithContext(ContextSnapshot snapshot, T delegate) {
        this(snapshot, delegate, null);
    }

    protected WrapperWithContext(final ContextSnapshot snapshot, final T delegate, final ContextSnapshotConsumer consumer) {
        this(new ContextSnapshotSupplier() {
            public ContextSnapshot get() {
                return snapshot;
            }
        }, delegate, consumer);
        if (snapshot == null) {
            throw new NullPointerException(String.format("No context snapshot provided to %s.", this));
        }
    }

    /**
     * Wraps the delegate and provides a context.
     * <p>
     * <strong>Note:</strong> <em>Make sure the supplier function does <strong>not</strong> obtain the context snapshot
     * from any threadlocal storage! The wrapper is designed to propagate contexts from one thread to another.
     * Therefore, the snapshot must be {@link nl.talsmasoftware.context.ContextManagers#createContextSnapshot() captured}
     * in the source thread and {@link ContextSnapshot#reactivate() reactivated} in the target thread.
     * If unsure, please use the
     * {@link #WrapperWithContext(ContextSnapshot, Object, ContextSnapshotConsumer) constructor with snapshot} instead.</em>
     *
     * @param supplier The supplier for the context snapshot.
     *                 This can be a straightforward 'holder' object or an ongoing background call.
     *                 Please do <strong>not</strong> make this supplier function access any {@code ThreadLocal} value,
     *                 as the wrapper is designed to propagate the snapshot from thread to thread!
     * @param delegate The delegate object to be wrapped.
     * @param consumer The consumer of a resulting context snapshot (optional, only required if the caller is interested in it).
     * @see #WrapperWithContext(ContextSnapshot, Object, ContextSnapshotConsumer)
     */
    protected WrapperWithContext(ContextSnapshotSupplier supplier, T delegate, ContextSnapshotConsumer consumer) {
        super(delegate);
        this.supplier = supplier;
        this.consumer = consumer;
        if (supplier == null) {
            throw new NullPointerException(String.format("No context snapshot supplier provided to %s.", this));
        }
    }

    /**
     * Calls the supplier for the context snapshot, making sure it is called only once for this wrapper instance.
     *
     * @return The snapshot value.
     */
    protected ContextSnapshot snapshot() {
        // Note: Double-checked locking works fine in decent JDKs
        if (_snapshot == null) synchronized (this) {
            if (_snapshot == null) _snapshot = supplier.get();
        }
        if (_snapshot == null) throw new NullPointerException("Context snapshot is <null>.");
        return _snapshot;
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
        try {
            return getClass().getSimpleName() + "{delegate=" + delegate() + ", snapshot=" + snapshot() + '}';
        } catch (NullPointerException ignored) {
            return getClass().getSimpleName() + "{delegate=" + delegate() + ", snapshot=<null>}";
        }
    }

}
