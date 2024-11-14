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
 * API concepts used throughout the {@code context-propagation} library.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.api.Context}</h2>
 * <p>
 * A {@linkplain nl.talsmasoftware.context.api.Context context} contains
 * a {@linkplain nl.talsmasoftware.context.api.Context#getValue() value}.<br>
 * There can be one active context per thread. A context remains active until it is closed or another context
 * is activated in the same thread. Normally, a context is backed by a {@linkplain java.lang.ThreadLocal ThreadLocal}
 * variable.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.api.ContextManager}</h2>
 * <p>
 * Manages the active context.
 * Can {@linkplain nl.talsmasoftware.context.api.ContextManager#initializeNewContext(java.lang.Object) initialize a new context}
 * and provides access to
 * the {@linkplain nl.talsmasoftware.context.api.ContextManager#getActiveContextValue() active context value}.
 *
 * <p>
 * For normal application code it should not be necessary to interact with context managers directly.
 * Instead, using the various context-aware utility classes from the core module will automatically propagate
 * any supported context types to background threads for you.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.api.ContextSnapshot}</h2>
 * <p>
 * A snapshot contains the active context value from all known context managers.<br>
 * These values can be reactivated all together in another thread
 * by {@linkplain nl.talsmasoftware.context.api.ContextSnapshot#reactivate() reactivating} the snapshot.<br>
 * Reactivated snapshots <strong>must</strong> be closed to avoid leaking context.
 *
 * <p>
 * All context aware utility classes in this the core module of this library are tested
 * to make sure they reactivate and close snapshots in a safe way.
 *
 * @author Sjoerd Talsma
 */
package nl.talsmasoftware.context.api;
