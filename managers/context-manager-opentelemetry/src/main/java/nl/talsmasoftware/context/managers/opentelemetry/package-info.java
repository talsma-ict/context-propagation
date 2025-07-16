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
 * Context Manager for the open telemetry {@linkplain io.opentelemetry.context.Context Context}.
 *
 * <p>
 * Includes the {@linkplain io.opentelemetry.context.Context#current() current}
 * open telemetry {@linkplain io.opentelemetry.context.Context Context}
 * in {@linkplain nl.talsmasoftware.context.api.ContextSnapshot#capture() captured}
 * {@linkplain nl.talsmasoftware.context.api.ContextSnapshot ContextSnapshot}s.
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
 * <h2>Bridge function</h2>
 * Besides capturing the current Context, this module
 * also {@linkplain io.opentelemetry.context.ContextStorage#addWrapper(java.util.function.Function) adds}
 * an {@linkplain nl.talsmasoftware.context.managers.opentelemetry.OpenTelemetryContextStorageWrapper OpenTelemetryContextStorageWrapper}
 * to the configured open telemetry {@linkplain io.opentelemetry.context.ContextStorage ContextStorage}.<br>
 * This wrapper includes captured {@linkplain nl.talsmasoftware.context.api.ContextSnapshot ContextSnapshot}s
 * into each Context returned from {@linkplain io.opentelemetry.context.Context#current()},
 * thereby bridging <em>all</em> supported {@linkplain nl.talsmasoftware.context.api.ContextManager} implementations
 * over the open telemetry {@linkplain io.opentelemetry.context.Context Context} mechanism.
 */
package nl.talsmasoftware.context.managers.opentelemetry;
