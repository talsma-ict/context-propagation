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
package nl.talsmasoftware.context.managers.springsecurity;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
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
public class SpringSecurityContextManager implements ContextManager<Authentication> {
    /**
     * Singleton instance of this class.
     */
    private static final SpringSecurityContextManager INSTANCE = new SpringSecurityContextManager();

    /**
     * Returns the singleton instance of the {@code SpringSecurityContextManager}.
     * <p>
     * The ServiceLoader supports a static {@code provider()} method to resolve services since Java 9.
     *
     * @return The Spring Security context manager.
     */
    public static SpringSecurityContextManager provider() {
        return INSTANCE;
    }

    /**
     * Creates a new context manager.
     *
     * @see #provider()
     * @deprecated This constructor only exists for usage by Java 8 {@code ServiceLoader}. The singleton instance
     * obtained from {@link #provider()} should be used to avoid unnecessary instantiations.
     */
    @Deprecated
    public SpringSecurityContextManager() {
    }

    /**
     * Creates a new Spring {@linkplain SecurityContext} and sets the {@linkplain Authentication value} in it.
     *
     * <p>
     * This new value is set in the {@linkplain SecurityContextHolder} and the current {@linkplain Authentication}
     * is remembered, to be restored when the returned {@link Context} is closed.
     *
     * @param authentication The value to initialize a new context for.
     * @return A context with the new Authentication, restoring the previous authentication when closed.
     */
    public Context<Authentication> initializeNewContext(Authentication authentication) {
        return new AuthenticationContext(authentication);
    }

    /**
     * @return A context object referring to the current {@code Authentication} in the spring security context holder.
     * Closing the returned context does nothing.
     */
    public Authentication getActiveContextValue() {
        return currentAuthentication();
    }

    private static Authentication currentAuthentication() {
        SecurityContext context = SecurityContextHolder.getContext();
        return context != null ? context.getAuthentication() : null;
    }

    /**
     * Clears the Spring {@linkplain SecurityContext}.
     *
     * @see SecurityContextHolder#clearContext()
     */
    public void clear() {
        SecurityContextHolder.clearContext();
    }

    private static final class AuthenticationContext implements Context<Authentication> {
        private final SecurityContext previous;
        private final AtomicBoolean closed;

        private AuthenticationContext(Authentication authentication) {
            this.previous = SecurityContextHolder.getContext();
            this.closed = new AtomicBoolean(false);
            setCurrentAuthentication(authentication);
        }

        private void setCurrentAuthentication(Authentication authentication) {
            SecurityContext current = SecurityContextHolder.createEmptyContext();
            if (authentication != null) {
                current.setAuthentication(authentication);
            }
            SecurityContextHolder.setContext(current);
        }

        public Authentication getValue() {
            return currentAuthentication();
        }

        public void close() {
            if (closed.compareAndSet(false, true)) {
                SecurityContextHolder.setContext(previous);
            }
        }
    }

}
