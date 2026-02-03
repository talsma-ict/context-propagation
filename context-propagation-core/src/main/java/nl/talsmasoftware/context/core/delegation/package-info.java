/*
 * Copyright 2016-2026 Talsma ICT
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
 * Delegation base classes.
 *
 * <p>
 * These base classes provide full implementations where behaviour is delegated to a wrapped delegate,
 * providing overridable <em>wrap</em> methods to influence behaviour in a consistent way.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.core.delegation.Wrapper Wrapper}</h2>
 * <p>
 * Base class for any delegation in this package.<br>
 * Wrappers provide an {@linkplain nl.talsmasoftware.context.core.delegation.Wrapper#delegate() accessor}
 * for the delegate object to their subclasses.<br>
 * Any wrapper contains an implementation of {@linkplain java.lang.Object#hashCode() hashCode},
 * {@linkplain java.lang.Object#equals(java.lang.Object) equals}
 * and {@linkplain java.lang.Object#toString() toString} based on the actual class and the wrapped instance.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.core.delegation.WrapperWithContext WrapperWithContext}</h2>
 * <p>
 * A wrapper containing also a {@linkplain nl.talsmasoftware.context.api.ContextSnapshot context snapshot}
 * besides a delegate.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.core.delegation.DelegatingFuture DelegatingFuture}</h2>
 * <p>
 * A {@linkplain java.util.concurrent.Future future} that delegates all operations,
 * allowing the result or error to be wrapped.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.core.delegation.DelegatingExecutorService DelegatingExecutorService}</h2>
 * <p>
 * An {@linkplain java.util.concurrent.ExecutorService ExecutorService} delegating all scheduling operations,
 * providing a consistent way to {@code wrap} the scheduled tasks and resulting futures.
 */
package nl.talsmasoftware.context.core.delegation;
