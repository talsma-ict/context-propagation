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
/**
 * Main API concepts used throughout the {@code context-propagation} library.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.api.Context}</h2>
 * <p>
 * A {@linkplain nl.talsmasoftware.context.api.Context context} contains
 * a {@linkplain nl.talsmasoftware.context.api.Context#getValue() value}.<br>
 * There can be one active context per thread. A context remains active until it is closed or another context
 * is activated in that thread.
 *
 * <p>
 * An {@code AbstractThreadLocalContext} base class is provided in the core module that supports
 * nested contexts and provides predictable behaviour for out-of-order closing.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.api.ContextManager}</h2>
 * <p>
 * Manages the active context.
 * Can {@linkplain nl.talsmasoftware.context.api.ContextManager#initializeNewContext(java.lang.Object) initialize a new context}
 * and provides access to
 * the {@linkplain nl.talsmasoftware.context.api.ContextManager#getActiveContextValue() active context value}.
 *
 * <p>
 * For most application code it should not be necessary to interact with context managers directly.
 * Instead, using the various context-aware utility classes will automatically propagate context values.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.api.ContextSnapshot}</h2>
 * <p>
 * A snapshot contains the current value from all known context managers.<br>
 * These values can be reactivated in another thread.<br>
 * Reactivated snapshots must be closed to avoid leaking context.
 *
 * <p>
 * All context aware utility classes in this library are tested
 * to make sure they reactivate and close snapshots in a safe way.
 *
 * @author Sjoerd Talsma
 */
package nl.talsmasoftware.context.api;
