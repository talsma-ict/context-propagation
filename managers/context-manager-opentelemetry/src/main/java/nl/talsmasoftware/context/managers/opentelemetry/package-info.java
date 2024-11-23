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
 * Manager to propagate {@linkplain io.opentelemetry.context.Context OpenTelemetry context}
 * from one thread to another.
 *
 * <p>
 * The ContextManager delegates {@linkplain java.lang.ThreadLocal ThreadLocal} management to the
 * default {@linkplain io.opentelemetry.context.Context OpenTelemetry Context} storage.<br>
 * <ul>
 *     <li>Obtaining the current context value is delegated to
 *     {@linkplain io.opentelemetry.context.Context#current()}.
 *     <li>Intializing a new context value is delegated to
 *     {@linkplain io.opentelemetry.context.Context#makeCurrent()}.
 * </ul>
 *
 * <p>
 * Adding the {@code context-manager-opentelemetry} library to the classpath
 * is all that is needed to include the {@link io.opentelemetry.context.Context OpenTelemetry Context}
 * in {@linkplain nl.talsmasoftware.context.api.ContextSnapshot ContextSnapshots}.
 * This propagates the context to other threads using the
 * {@code ContextAwareExecutorService} or {@code ContextAwareCompletableFuture}.
 *
 * <p>
 * Also, any function <em>..WithContext</em> in the {@code nl.talsmasoftware.context.core.function} package
 * automatically activates the context snapshot around the function body.
 */
package nl.talsmasoftware.context.managers.opentelemetry;
