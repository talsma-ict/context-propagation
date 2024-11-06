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
 * Java 8 functional interface wrappers that apply context snapshots to the mapped functions.
 *
 * <p>
 * The following functional interfaces are available:
 * <ul>
 * <li>{@link nl.talsmasoftware.context.functions.BiConsumerWithContext}
 * <li>{@link nl.talsmasoftware.context.functions.BiFunctionWithContext}
 * <li>{@link nl.talsmasoftware.context.functions.BinaryOperatorWithContext}
 * <li>{@link nl.talsmasoftware.context.functions.BiPredicateWithContext}
 * <li>{@link nl.talsmasoftware.context.functions.BooleanSupplierWithContext}
 * <li>{@link nl.talsmasoftware.context.functions.ConsumerWithContext}
 * <li>{@link nl.talsmasoftware.context.functions.FunctionWithContext}
 * <li>{@link nl.talsmasoftware.context.functions.PredicateWithContext}
 * <li>{@link nl.talsmasoftware.context.functions.RunnableWithContext}
 * <li>{@link nl.talsmasoftware.context.functions.SupplierWithContext}
 * <li>{@link nl.talsmasoftware.context.functions.UnaryOperatorWithContext}
 * </ul>
 *
 * <p>
 * The base class {@link nl.talsmasoftware.context.functions.WrapperWithContextAndConsumer} allows
 * capturing a new context snapshot after the function has finished.
 */
package nl.talsmasoftware.context.functions;