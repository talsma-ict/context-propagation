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
 * Manager to propagate {@linkplain io.opentracing.Span OpenTracing span} from one thread to another.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.managers.opentracing.SpanManager}</h2>
 * Management of {@linkplain io.opentracing.Span spans} is delegated by
 * {@linkplain nl.talsmasoftware.context.managers.opentracing.SpanManager SpanManager} to the
 * configured {@linkplain io.opentracing.util.GlobalTracer}.
 * <ul>
 *     <li>Obtaining the current context value is delegated to
 *     {@linkplain io.opentracing.Tracer#activeSpan() active span}.
 *     <li>Intializing a new context value is delegated to
 *     {@linkplain io.opentracing.util.GlobalTracer#activateSpan(Span)}.
 * </ul>
 *
 * <p>
 * Adding the {@code context-manager-opentracing} library to the classpath will automatically
 * include the {@linkplain io.opentracing.Span active Span}
 * in {@linkplain nl.talsmasoftware.context.api.ContextSnapshot ContextSnapshots}.
 * This propagates the context to other threads using the
 * {@code ContextAwareExecutorService} or {@code ContextAwareCompletableFuture}.
 *
 * <p>
 * Also, any function <em>..WithContext</em> in the {@code nl.talsmasoftware.context.core.function} package
 * automatically activates the context snapshot around the function body.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.managers.opentracing.ContextScopeManager}</h2>
 * Our own ThreadLocal {@linkplain io.opentracing.ScopeManager} implementation.
 *
 * <p>
 * Using it is optional. It will <strong>not</strong> be configured automatically.
 * Check the configuration of your preferred tracer if it allows configuring a custom scope manager.
 */
package nl.talsmasoftware.context.managers.opentracing;

import io.opentracing.Span;
