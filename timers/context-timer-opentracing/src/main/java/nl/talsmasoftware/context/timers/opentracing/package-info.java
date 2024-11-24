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
 * {@link nl.talsmasoftware.context.api.ContextTimer Context timer} that
 * creates a {@linkplain io.opentracing.Span Span}
 * using the {@linkplain io.opentracing.util.GlobalTracer GlobalTracer}
 * for context switches.
 *
 * <p>
 * Individual {@linkplain nl.talsmasoftware.context.api.ContextManager context managers}
 * are <strong>not</strong> traced, only the operations
 * regarding {@linkplain nl.talsmasoftware.context.api.ContextSnapshot ContextSnapshot}
 * are traced.
 */
package nl.talsmasoftware.context.timers.opentracing;

