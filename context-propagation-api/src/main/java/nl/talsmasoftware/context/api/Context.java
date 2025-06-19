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
 * A context can be anything that needs to be maintained on the 'current thread' level.
 *
 * <p>
 * A new context is created by {@link ContextManager#activate(Object) activating}
 * it using a {@link ContextManager}.
 *
 * <h2>Important!</h2>
 * It is the responsibility of the one activating a new context to also {@linkplain #close()} it again
 * <em>from the same thread</em>.<br>
 * Using every activated context in a 'try-with-resources' block of code is a recommended and safe way
 * to make sure this responsibility is honored.
 *
 * <p>
 * This library provides an {@code AbstractThreadLocalContext} that provides
 * <ul>
 *     <li>Random-depth nested contexts.
 *     <li>Restoration of 'previous' context state when closing.
 *     <li>Unwinding to the nearest un-closed context in case contexts get closed out-of-sequence.
 *     Closing out-of-sequence will not happen if all contexts are used in try-with-resources blocks,
 *     but unwinding provides consistent behaviour in case it does happen.
 * </ul>
 *
 * @author Sjoerd Talsma
 * @since 2.0.0
 */
public interface Context extends Closeable {

    /**
     * Closes this context.
     *
     * <p>
     * It is the responsibility of the one activating a new context to also close it again <em>from the same thread</em>.
     *
     * <p>
     * It must be possible to call this method multiple times.
     * Subsequent {@code close()} calls must have no effect and should not throw exceptions.
     *
     * <p>
     * Implementors are advised to restore previous contextual state upon close but are not obliged to do so.
     */
    void close();

}
