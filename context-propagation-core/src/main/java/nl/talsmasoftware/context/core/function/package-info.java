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
 * Functional interface wrappers that apply context snapshots to the mapped functions.
 *
 * <p>
 * The following functional interfaces are available:
 * <ul>
 * <li>{@link nl.talsmasoftware.context.core.function.BiConsumerWithContext}
 * <li>{@link nl.talsmasoftware.context.core.function.BiFunctionWithContext}
 * <li>{@link nl.talsmasoftware.context.core.function.BinaryOperatorWithContext}
 * <li>{@link nl.talsmasoftware.context.core.function.BiPredicateWithContext}
 * <li>{@link nl.talsmasoftware.context.core.function.BooleanSupplierWithContext}
 * <li>{@link nl.talsmasoftware.context.core.function.ConsumerWithContext}
 * <li>{@link nl.talsmasoftware.context.core.function.FunctionWithContext}
 * <li>{@link nl.talsmasoftware.context.core.function.PredicateWithContext}
 * <li>{@link nl.talsmasoftware.context.core.function.RunnableWithContext}
 * <li>{@link nl.talsmasoftware.context.core.function.SupplierWithContext}
 * <li>{@link nl.talsmasoftware.context.core.function.UnaryOperatorWithContext}
 * </ul>
 *
 * <p>
 * The base class {@link nl.talsmasoftware.context.core.function.WrapperWithContextAndConsumer} allows
 * capturing a new context snapshot after the function has finished.
 */
package nl.talsmasoftware.context.core.function;
