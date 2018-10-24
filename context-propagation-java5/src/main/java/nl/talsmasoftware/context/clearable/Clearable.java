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
package nl.talsmasoftware.context.clearable;

/**
 * Interface that marks an to be 'clearable'.
 * <p>
 * Context managers that support clearing the active context for the current thread
 * should implement the {@link Clearable} interface.
 * <p>
 * Clearing the context removes not only an active context from the current thread,
 * but also the potential stack of any parents.
 * This operation is intended to only be used when re-using threads (e.g. when returning them to a thread-pool).
 *
 * @author Sjoerd Talsma
 */
public interface Clearable {

    /**
     * Clears the current context and any potential parent contexts that may be stacked.
     */
    void clear();

}
