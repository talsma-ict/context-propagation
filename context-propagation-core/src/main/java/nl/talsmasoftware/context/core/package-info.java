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
 * Core implementation classes of this library.
 *
 * <p>
 * All context aware utility classes in this library are tested to make sure they reactivate and close snapshots in a safe way.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.core.concurrent concurrent}</h2>
 * <p>
 * Concurrent classes that will automatically propagate a context snapshot into other threads,
 * making sure the reactivation gets closed after the work is done.
 * <ul>
 *     <li>{@linkplain nl.talsmasoftware.context.core.concurrent.ContextAwareExecutorService ContextAwareExecutorService}
 *     that wraps an existing {@code ExecutorService},
 *     propagating a {@linkplain nl.talsmasoftware.context.api.ContextSnapshot context snapshot} for every submitted task.
 *     <li>{@linkplain nl.talsmasoftware.context.core.concurrent.ContextAwareCompletableFuture ContextAwareCompletableFuture}
 *     that propagates the {@linkplain nl.talsmasoftware.context.api.ContextSnapshot context snapshot}
 *     across its completion stages.
 * </ul>
 *
 * <h2>{@linkplain nl.talsmasoftware.context.core.function function}</h2>
 * <p>
 * Functional interfaces that reactivate a {@linkplain nl.talsmasoftware.context.api.ContextSnapshot context snapshot}
 * during their execution and safely close the reactivation after the work is done.
 * <ul>
 *     <li>{@linkplain nl.talsmasoftware.context.core.function.RunnableWithContext RunnableWithContext}
 *     <li>{@linkplain nl.talsmasoftware.context.core.function.BiConsumerWithContext BiConsumerWithContext}
 *     <li>{@linkplain nl.talsmasoftware.context.core.function.BiFunctionWithContext BiFunctionWithContext}
 *     <li>{@linkplain nl.talsmasoftware.context.core.function.BinaryOperatorWithContext BinaryOperatorWithContext}
 *     <li>{@linkplain nl.talsmasoftware.context.core.function.BiPredicateWithContext BiPredicateWithContext}
 *     <li>{@linkplain nl.talsmasoftware.context.core.function.BooleanSupplierWithContext BooleanSupplierWithContext}
 *     <li>{@linkplain nl.talsmasoftware.context.core.function.ConsumerWithContext ConsumerWithContext}
 *     <li>{@linkplain nl.talsmasoftware.context.core.function.FunctionWithContext FunctionWithContext}
 *     <li>{@linkplain nl.talsmasoftware.context.core.function.PredicateWithContext PredicateWithContext}
 *     <li>{@linkplain nl.talsmasoftware.context.core.function.SupplierWithContext SupplierWithContext}
 *     <li>{@linkplain nl.talsmasoftware.context.core.function.UnaryOperatorWithContext UnaryOperatorWithContext}
 * </ul>
 *
 * <h2>{@linkplain nl.talsmasoftware.context.core.delegation delegation}</h2>
 * <p>
 * Base classes that are easy to extend by overriding one or only a few methods
 * without having to manually delegate all standard behaviour to a wrapped delegate.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.core.threadlocal threadlocal}</h2>
 * <p>
 * ThreadLocal utilities.
 * <ul>
 *     <li>{@linkplain nl.talsmasoftware.context.core.threadlocal.AbstractThreadLocalContext AbstractThreadLocalContext}
 *     providing a base class to extend, providing:
 *     <ul>
 *         <li>A {@linkplain java.lang.ThreadLocal ThreadLocal} instance per subclass.
 *         <li>Predictable behaviour on out-of-sequence closing. Even though this should not happen with correct use
 *         of the library, it is a good idea to be resilient when things go different anyway.
 *         Our implementation guarantees that no closed context is ever propagated to another thread.
 *     </ul>
 * </ul>
 *
 * @author Sjoerd Talsma
 */
package nl.talsmasoftware.context.core;
