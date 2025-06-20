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
package nl.talsmasoftware.context.api;

import java.io.Closeable;

/**
 * Abstraction for an activated {@linkplain ThreadLocal} value.
 *
 * <p>
 * When the context manager {@linkplain ContextManager#activate(Object) activates} a value,
 * a new Context is returned. Closing this context will remove the activated value again.
 *
 * <h2>Important!</h2>
 * It is the responsibility of the one activating a new Context to also {@linkplain #close()} it
 * <em>from the same thread</em>.<br>
 * Using every activated context in a 'try-with-resources' block of code is a recommended and safe way
 * to make sure this responsibility is honored.
 *
 * <p>
 * The {@code context-propagation-core} module provides an {@code AbstractThreadLocalContext} base class
 * that features nesting active values and predictable behaviour for out-of-order closing.
 *
 * @author Sjoerd Talsma
 * @since 2.0.0
 */
public interface Context extends Closeable {

    /**
     * Close this context by removing the activated value.
     *
     * <p>
     * It is the responsibility of the one activating a new context to also close it <em>from the same thread</em>.
     *
     * @implNote Implementors are advised to <em>restore</em> the previous thread-local value upon close, but are not obliged to do so.
     * @implSpec It <strong>must</strong> be possible to call the {@linkplain #close()} method multiple times. Later calls must have no effect and should not throw exceptions.
     */
    void close();

}
