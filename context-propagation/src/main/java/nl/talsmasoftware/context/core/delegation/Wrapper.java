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

/**
 * Base class wrapping a {@linkplain #delegate()} object.
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
     * The wrapped delegate.
     *
     * <p>
     * Every wrapper <strong>must</strong> always return a non-{@code null} delegate.<br>
     * By default, the delegate value from the constructor is returned.<br>
     * If the delegate is not yet available when the wrapper is constructed,
     * this method <strong>must</strong> be overridden.
     *
     * @return The delegate for this wrapper.
     */
    protected T delegate() {
        return delegate;
    }

    /**
     * Determines if this class is a wrapper of the specified object.
     *
     * @param other The object to check for being the delegate of this wrapper.
     * @return {@code true} is this wrapper has the given object as its delegate.
     */
    public boolean isWrapperOf(T other) {
        return delegate().equals(other);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other != null && getClass().equals(other.getClass()) && delegate().equals(((Wrapper) other).delegate()));
    }

    /**
     * @return The class name and the delegate string representation.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + delegate() + '}';
    }
}
