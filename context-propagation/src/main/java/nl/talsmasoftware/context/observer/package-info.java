/*
 * Copyright 2016-2021 Talsma ICT
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
 * Observe context changes
 *
 * <h2>{@linkplain nl.talsmasoftware.context.observer.ContextObserver}</h2>
 * <p>
 * Interface that can be implemented to observe context changes.
 *
 * <p>
 * Each call to {@linkplain nl.talsmasoftware.context.ContextManager#initializeNewContext(java.lang.Object)} or
 * {@linkplain nl.talsmasoftware.context.Context#close()} is sent to all relevant context observers.
 *
 * <p>
 * Each observer must specify which context manager is being observed by implementing the
 * {@linkplain nl.talsmasoftware.context.observer.ContextObserver#getObservedContextManager()} method.
 * Context observers are looked up by java's {@code ServiceLoader}.
 */
package nl.talsmasoftware.context.observer;