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
 * <strong>(Deprecated)</strong> The {@code Clearable} interface and a {@code ClearableContextManager} in particular.
 *
 * <h2>Deprecated</h2>
 * This package will be removed. Method clear() will be added to the ContextManager interface.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.clearable.Clearable}</h2>
 * <p>
 * This interface declares the {@linkplain nl.talsmasoftware.context.clearable.Clearable#clear()} method.
 * Clearing something is semantically different from closing.
 *
 * <p>
 * In the case of {@linkplain nl.talsmasoftware.context.clearable.ClearableContextManager},
 * closing any context only applies to that single context,
 * while {@linkplain nl.talsmasoftware.context.clearable.ClearableContextManager#clear()} is
 * equivalent to closing the active context and <em>all</em> parents in the current thread.
 */
package nl.talsmasoftware.context.clearable;
