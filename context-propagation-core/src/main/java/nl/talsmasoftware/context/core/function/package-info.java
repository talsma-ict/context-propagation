/*
 * Copyright 2016-2026 Talsma ICT
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
 * Functional interface wrappers that apply context snapshots to the mapped functions.
 *
 * <p>
 * The following context-aware java functional interface equivalents are available:
 * <ul>
 * <li>{@link nl.talsmasoftware.context.core.function.RunnableWithContext}
 * Runnable that reactivates a context snapshot during the task.
 * <li>{@link nl.talsmasoftware.context.core.function.SupplierWithContext}
 * Supplier that reactivates a context snapshot while getting the value.
 * <li>{@link nl.talsmasoftware.context.core.function.ConsumerWithContext}
 * Consumer that reactivates a context snapshot while passing the value.
 * <li>{@link nl.talsmasoftware.context.core.function.FunctionWithContext}
 * Function that reactivates a context snapshot when applying the function.
 * <li>{@link nl.talsmasoftware.context.core.function.PredicateWithContext}
 * Predicate that reactivates a context snapshot when testing the predicate.
 * <li>{@link nl.talsmasoftware.context.core.function.UnaryOperatorWithContext}
 * Operator that reactivates a context snapshot when applying the operator.
 * <li>{@link nl.talsmasoftware.context.core.function.BinaryOperatorWithContext}
 * Binary operator that reactivates a context snapshot when applying the operator.
 * <li>{@link nl.talsmasoftware.context.core.function.BiFunctionWithContext}
 * BiFunction that reactivates a context snapshot when applying the function.
 * <li>{@link nl.talsmasoftware.context.core.function.BiConsumerWithContext}
 * BiConsumer that reactivates a context snapshot while passing the values.
 * <li>{@link nl.talsmasoftware.context.core.function.BiPredicateWithContext}
 * BiPredicate that reactivates a context snapshot while passing the values.
 * <li>{@link nl.talsmasoftware.context.core.function.BooleanSupplierWithContext}
 * Boolean supplier that reactivates a context snapshot while getting the outcome.
 * </ul>
 */
package nl.talsmasoftware.context.core.function;
