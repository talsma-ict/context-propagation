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

import nl.talsmasoftware.context.api.ContextSnapshot;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A {@linkplain Wrapper wrapper} containing a {@linkplain #snapshot() context snapshot}
 * besides a {@linkplain #delegate() delegate}.
 *
 * @author Sjoerd Talsma
 */
public abstract class WrapperWithContext<T> extends Wrapper<T> {

    private final Supplier<ContextSnapshot> supplier;
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
        this.snapshot = requireNonNull(snapshot, () -> "No context snapshot provided to " + this + '.');
    }

    /**
     * Wraps the delegate and provides a context snapshot.
     * <p>
     * <strong>Note:</strong> <em>Make sure the supplier function does <strong>not</strong> obtain the context snapshot
     * from any threadlocal storage! The wrapper is designed to propagate contexts from one thread to another.
     * Therefore, the snapshot must be {@link ContextSnapshot#capture() captured}
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
    protected WrapperWithContext(Supplier<ContextSnapshot> supplier, T delegate) {
        super(delegate);
        this.supplier = requireNonNull(supplier, () -> "No context snapshot supplier provided to " + this + '.');
        this.snapshot = null;
    }

    /**
     * Calls the supplier for the context snapshot, making sure it is called only once for this wrapper instance.
     *
     * @return The snapshot value.
     */
    protected ContextSnapshot snapshot() {
        // Note: Double-checked locking works fine in modern JDKs
        // Discussion here: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590407
        if (snapshot == null && supplier != null) {
            synchronized (this) {
                if (snapshot == null) {
                    snapshot = requireNonNull(supplier.get(), "Context snapshot is <null>.");
                }
            }
        }
        return snapshot;
    }

    /**
     * Returns a hash code value for the object.
     *
     * <p>
     * The hash code is based on:
     * <ul>
     *     <li>The hash code of the delegate.
     *     <li>The hash code of the snapshot.
     * </ul>
     *
     * @return Hashcode based on the hascodes of both the delegate and context snapshot.
     * @see #delegate()
     * @see #snapshot()
     */
    @Override
    public int hashCode() {
        return 31 * super.hashCode() + snapshot().hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * <p>
     * Another object is considered equal to a wrapper if:
     * <ul>
     *     <li>The other object is an instance of the same {@code Wrapper} class.
     *     <li>The delegate is equal to the delegate of the other object.
     *     <li>The context snapshot is equal to the snapshot of the other context object.</li>
     * </ul>
     *
     * @param other The other object to compare with.
     * @return {@code true} if the other object is the same wrapper class and its delegate and context snapshots are equal.
     * @see #delegate()
     * @see #snapshot()
     */
    @Override
    public boolean equals(Object other) {
        return this == other || (super.equals(other) && snapshot().equals(((WrapperWithContext<?>) other).snapshot()));
    }

    /**
     * Returns a string representation of the object.
     *
     * @return The class name, delegate representation and
     * optionally the context snapshot (only if it was already eagerly evaluated).
     */
    @Override
    public String toString() {
        return snapshot == null ? super.toString()
                : getClass().getSimpleName() + "{delegate=" + delegate() + ", " + snapshot + "}";
    }

}
