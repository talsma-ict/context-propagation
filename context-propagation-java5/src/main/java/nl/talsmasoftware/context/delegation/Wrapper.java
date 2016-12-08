/*
 * Copyright (C) 2016 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.talsmasoftware.context.delegation;

/**
 * Base wrapper class offering a {@link #nonNullDelegate() non-null delegate} method.
 *
 * @author Sjoerd Talsma
 */
public abstract class Wrapper<T> {

    private final T delegate;

    /**
     * Constructor providing a delegate wrapped object.
     *
     * @param delegate The delegate object being wrapped.
     *                 This may <strong>only</strong> be <code>null</code> if the <code>delegate()</code> method is
     *                 overridden to provide an alternative non-<code>null</code> result.
     */
    protected Wrapper(T delegate) {
        this.delegate = delegate;
    }

    /**
     * Delegate method that can be overridden in case the delegate is not yet available at construction time or when
     * there some strategy applicable that determines the delegate at runtime.
     * <p>
     * By default, the specified delegate value from the constructor is returned.
     *
     * @return The delegate for this wrapper.
     */
    protected T delegate() {
        return delegate;
    }

    /**
     * Acessor to provide a guaranteed non-<code>null</code> delegate instance for use within delegation method
     * implementations.
     *
     * @return The delegate for this wrapper (guaranteed to be non-<code>null</code>).
     * @throws NullPointerException with a specific message in case the delegate was null.
     */
    protected final T nonNullDelegate() {
        final T foundDelegate = delegate();
        if (foundDelegate == null) try {
            throw new NullPointerException(String.format("No delegate available for %s.", this));
        } catch (StackOverflowError toStringCallsNonNullDelegate) {
            throw new NullPointerException(String.format("No delegate available for %s.", getClass().getSimpleName()));
        }
        return foundDelegate;
    }

    @Override
    public int hashCode() {
        return delegate == null ? super.hashCode() : delegate.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other ||
                (other != null
                        && delegate != null
                        && getClass().equals(other.getClass())
                        && delegate.equals(((Wrapper) other).delegate));
    }

    static boolean equals(Object obj1, Object obj2) {
        return obj1 == obj2 || (obj1 != null && obj2 != null && obj1.equals(obj2));
    }

    /**
     * @return The class name and the delegate string representation.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{delegate=" + delegate() + '}';
    }

}
