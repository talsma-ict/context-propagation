/*
 * Copyright 2016-2018 Talsma ICT
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
package nl.talsmasoftware.context.springsecurity;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.clearable.ClearableContextManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A context manager that propagates spring-security {@link Authentication} objects
 * into background threads using the {@code ContextAwareExecutorService}.
 * <p>
 * Management of the authentication is fully delegated to the Spring {@link SecurityContextHolder},
 * so no additional {@link java.lang.ThreadLocal} variables are used.
 *
 * @author Sjoerd Talsma
 */
public class SpringSecurityContextManager implements ClearableContextManager<Authentication> {

    /**
     * Creates a new Spring {@linkplain SecurityContext} and sets the {@linkplain Authentication value} in it.
     * <p>
     * This new value is set in the {@linkplain SecurityContextHolder} and the current {@linkplain Authentication}
     * is remembered, to be restored when the returned {@link Context} is closed.
     *
     * @param value The value to initialize a new context for.
     * @return A context with the new Authentication, restoring the previous authentication when closed.
     */
    public Context<Authentication> initializeNewContext(Authentication value) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext current = SecurityContextHolder.createEmptyContext();
        current.setAuthentication(value);
        SecurityContextHolder.setContext(current);
        return new AuthenticationContext(current, previous, false);
    }

    /**
     * @return A context object referring to the current {@code Authentication} in the spring security context holder.
     * Closing the returned context does nothing.
     */
    public Context<Authentication> getActiveContext() {
        return new AuthenticationContext(SecurityContextHolder.getContext(), null, true);
    }

    /**
     * Clears the Spring {@linkplain SecurityContext} by calling {@linkplain SecurityContextHolder#clearContext()}.
     */
    public void clear() {
        SecurityContextHolder.clearContext();
    }

    private static final class AuthenticationContext implements Context<Authentication> {
        private volatile SecurityContext current;
        private final SecurityContext previous;
        private final AtomicBoolean closed;

        private AuthenticationContext(SecurityContext current, SecurityContext previous, boolean alreadyClosed) {
            this.current = current;
            this.previous = previous;
            this.closed = new AtomicBoolean(alreadyClosed);
        }

        public Authentication getValue() {
            return current == null ? null : current.getAuthentication();
        }

        public void close() {
            if (closed.compareAndSet(false, true)) {
                SecurityContextHolder.setContext(previous);
            }
        }
    }

}
