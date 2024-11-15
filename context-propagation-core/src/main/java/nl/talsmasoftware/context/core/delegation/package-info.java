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
 * Base classes for delegation.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.core.delegation.Wrapper Wrapper}</h2>
 * <p>
 * Base class for any delegation in this package.<br>
 * Wrappers provide an accessors for the delegate object to their subclasses:
 * {@linkplain nl.talsmasoftware.context.core.delegation.Wrapper#delegate() delegate()}.<br>
 * Any wrapper further contains an implementation of {@code hashCode}, {@code equals}
 * and {@code toString} based on the actual class and the wrapped instance.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.core.delegation.WrapperWithContext WrapperWithContext}</h2>
 * <p>
 * Just a wrapper that also contains either a context snapshot or a supplier for it.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.core.delegation.DelegatingFuture DelegatingFuture}</h2>
 * <p>
 * Future that passes all operations to a delegate future.<br>
 * It allows the result of the to future to be wrapped using an overridable  {@code wrapResult} method.
 */
package nl.talsmasoftware.context.core.delegation;
