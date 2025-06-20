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
package nl.talsmasoftware.context.managers.slf4j.mdc;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.Map;

/**
 * Manager to propagate the Slf4J {@linkplain MDC MDC context map} from one thread to another.
 *
 * <p>
 * As {@link MDC} already manages its own thread-local state,
 * getting the active context is fully delegated to the MDC.
 *
 * <p>
 * Closing a context returned form {@link #activate(Map)} restores the MDC
 * to the values it had before the context was created.<br>
 * This means that closing nested contexts out-of-order will probably result in an undesirable state.<br>
 * It is therefore strongly advised to use Java's {@code try-with-resources} mechanism to ensure proper
 * closing of nested MDC contexts.
 *
 * <p>
 * This manager does not implement the optional {@link #clear()} method.
 * {@linkplain ContextManager#clearAll()} will therefore <strong>not</strong> clear the {@linkplain MDC}.
 * Please use {@linkplain MDC#clear()} explicitly to do that.
 *
 * @author Sjoerd Talsma
 */
public class Slf4jMdcManager implements ContextManager<Map<String, String>> {
    /**
     * Singleton instance of this class.
     */
    @SuppressWarnings("java:S1874") // This is the singleton instance the constructor was deprecated for.
    private static final Slf4jMdcManager INSTANCE = new Slf4jMdcManager();

    /**
     * Returns the singleton instance of the {@code Slf4jMdcManager}.
     * <p>
     * The ServiceLoader supports a static {@code provider()} method to resolve services since Java 9.
     *
     * @return The Slf4j {@code MDC} manager.
     */
    public static Slf4jMdcManager provider() {
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
    @SuppressWarnings("java:S1133") // Code can only be removed if this library ever switches to Java 9+ compatibility.
    public Slf4jMdcManager() {
        super(); // no-op, default constructor for explicit deprecation.
    }

    /**
     * Activate a new MDC context populated by the specified values.
     *
     * <p>
     * The given values will become the active MDC values for the current thread.<br>
     * Closing the resulting context will restore the MDC to the state it had just before this call.
     *
     * <p>
     * Please be aware that this may overwrite changes made to the MDC from other code.
     *
     * @param mdcValues The values to activate a new context for
     *                  (which must be closed by the caller at the end of its lifecycle).
     * @return A context that -when closed- will restore the active MDC values to what they were just before this call.
     */
    public Context activate(final Map<String, String> mdcValues) {
        return new Slf4jMdcContext(mdcValues == null ? Collections.emptyMap() : mdcValues);
    }

    /**
     * Returns the active MDC values from the current thread.
     *
     * @return The active MDC values.
     * @see MDC#getCopyOfContextMap()
     */
    public Map<String, String> getActiveContextValue() {
        return MDC.getCopyOfContextMap();
    }

    /**
     * This manager does not support clearing the MDC.
     *
     * <p>
     * Calling {@linkplain ContextManager#clearAll()} will therefore <strong>not</strong> clear the
     * MDC by default. This can be achieved by calling {@link MDC#clear()} explicitly.
     *
     * @see MDC#clear()
     */
    public void clear() {
        // no-op
    }

    /**
     * String representation of this context manager.
     *
     * @return The simple class name since this manager of itself contains no state.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
