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
 * Layered code for delegation such as {@code CallMappingExecutorService}.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.delegation.Wrapper}</h2>
 * <p>
 * The base class for any delegation in this package.
 * Any wrapper contains an implementation of {@code hashCode}, {@code equals}
 * and {@code toString} based on the actual class and the wrapped instance.
 * A wrapper has two accessors for the delegate object: {@code delegate()}
 * and {@code nonNullDelegate()}. The latter validates that the delegate is non-null.
 *
 * <p>
 * The {@linkplain nl.talsmasoftware.context.delegation.WrapperWithContext} is
 * just a wrapper that also contains a context snapshot (or a supplier for it).
 *
 * <h2>{@linkplain nl.talsmasoftware.context.delegation.CallMappingExecutorService}</h2>
 * <p>
 * A {@linkplain nl.talsmasoftware.context.delegation.DelegatingExecutorService} that maps
 * all {@linkplain java.lang.Runnable} tasks in {@linkplain java.util.concurrent.Callable}
 * objects, providing a base executor service that can wrap any background task
 * by implementing a single {@code map(Callable)} method.
 */
package nl.talsmasoftware.context.delegation;