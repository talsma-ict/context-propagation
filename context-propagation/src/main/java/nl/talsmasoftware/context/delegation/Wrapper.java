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
package nl.talsmasoftware.context.delegation;

/**
 * Base class for wrapping a {@linkplain #delegate()} object.
 *
 * @author Sjoerd Talsma
 * @deprecated Moved to package {@code nl.talsmasoftware.context.core.delegation}.
 */
@Deprecated
public abstract class Wrapper<T> extends nl.talsmasoftware.context.core.delegation.Wrapper<T> {

    /**
     * Constructor providing a delegate wrapped object.
     *
     * @param delegate The delegate object being wrapped.
     *                 This may <strong>only</strong> be <code>null</code> if the <code>delegate()</code> method is
     *                 overridden to provide an alternative non-<code>null</code> result.
     */
    protected Wrapper(T delegate) {
        super(delegate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected T delegate() {
        return super.delegate();
    }

    /**
     * Accessor to provide a guaranteed non-<code>null</code> delegate instance for use within delegation method
     * implementations.
     *
     * @return The delegate for this wrapper (guaranteed to be non-<code>null</code>).
     * @throws NullPointerException with a specific message in case the delegate was null.
     * @deprecated This extra check will be removed in the next version,
     * {@linkplain #delegate()} <strong>must</strong> return non-null.
     */
    @Deprecated
    protected final T nonNullDelegate() {
        final T foundDelegate = delegate();
        if (foundDelegate == null) try {
            throw new NullPointerException(String.format("No delegate available for %s.", this));
        } catch (StackOverflowError toStringCallsNonNullDelegate) {
            throw new NullPointerException(String.format("No delegate available for %s.", getClass().getSimpleName()));
        }
        return foundDelegate;
    }
}
