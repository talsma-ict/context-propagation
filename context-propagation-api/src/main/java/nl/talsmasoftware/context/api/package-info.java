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
/**
 * The API of the {@code context-propagation} library.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.api.ContextSnapshot}</h2>
 * <p>
 * Captures active values from all detected {@linkplain nl.talsmasoftware.context.api.ContextManager} implementations.
 *
 * <p>
 * The captured values in a snapshot can be reactivated in another thread.<br>
 * Snapshot reactivations must be closed again to avoid leaking context.
 *
 * <p>
 * All context aware utility classes in this library are tested
 * to make sure they reactivate and close snapshots in a safe way.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.api.ContextManager}</h2>
 * <p>
 * Manages {@linkplain nl.talsmasoftware.context.api.Context contexts} by providing a standard way of interacting
 * with {@linkplain java.lang.ThreadLocal} values.
 *
 * <p>
 * Managed context values can be accessed via a ContextManager by:
 * <ul>
 *  <li>Calling {@code getActiveContextValue()} which <em>gets</em> the current thread-local value.
 *  <li>Calling {@code activate(value)} which <em>sets</em> the given value until {@code close()}
 *  is called on the returned {@code Context}.
 *  <li>Calling {@code clear()} which <em>removes</em> the thread-local value.
 * </ul>
 *
 * <h2>{@linkplain nl.talsmasoftware.context.api.Context}</h2>
 * <p>
 * Abstraction for an activated {@linkplain java.lang.ThreadLocal} value.
 *
 * <p>
 * When the context manager {@code activates} a value, a new Context is returned.
 * Closing this context will remove the activated value again.
 *
 * @author Sjoerd Talsma
 */
package nl.talsmasoftware.context.api;
