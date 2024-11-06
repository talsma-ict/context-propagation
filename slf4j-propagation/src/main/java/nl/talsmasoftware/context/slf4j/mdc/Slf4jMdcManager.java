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
package nl.talsmasoftware.context.slf4j.mdc;

import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.core.ContextManagers;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager to propagate the SLF4J {@link MDC} content from one thread to another.
 *
 * <p>
 * As {@link MDC} already manages its own thread-local state,
 * getting the active context is 100% delegated to the MDC.
 *
 * <p>
 * Closing a context returned form {@link #initializeNewContext(Map)} restores the MDC
 * to the values it had before the context was created.<br>
 * This means that closing nested contexts out-of-order will probably result in an undesirable state.<br>
 * It is therefore strongly advised to use Java's {@code try-with-resources} mechanism to ensure proper
 * closing when nesting new MDC contexts.
 *
 * <p>
 * This manager does not implement the optional {@link #clear()} method.
 * {@link ContextManagers#clearActiveContexts()} will therefore <strong>not</strong> clear the {@linkplain MDC}.
 * Please use {@linkplain MDC#clear()} explicitly to do that.
 *
 * @author Sjoerd Talsma
 */
public class Slf4jMdcManager implements ContextManager<Map<String, String>> {

    /**
     * Initializes a new MDC context populated by the specified values.
     * <p>
     * The given values will become the active MDC values for the current thread.<br>
     * Closing the resulting context will restore the MDC to the state it had just before this call.
     * <p>
     * Please be aware that this may overwrite changes made to the MDC from other code.
     *
     * @param mdcValues The values to initialize a new context for
     *                  (which must be closed by the caller at the end of its lifecycle).
     * @return A context that -when closed- will restore the active MDC values to what they were just before this call.
     */
    public Context<Map<String, String>> initializeNewContext(final Map<String, String> mdcValues) {
        return new Slf4jMdcContext(mdcValues);
    }

    /**
     * Returns the active MDC values from the current thread.
     * <p>
     * <strong>Please note:</strong> <em>Because these values are managed by MDC itself and not by us,
     * closing the resulting context has no effect.</em>
     *
     * @return Context containing the active MDC values.
     */
    public Map<String, String> getActiveContextValue() {
        return MDC.getCopyOfContextMap();
    }

    /**
     * This manager does not support clearing the MDC.
     *
     * <p>
     * Calling {@code ContextManagers.clearActiveContexts()} will therefore <strong>not</strong> clear the
     * MDC by default. This can be achieved by calling {@link MDC#clear()} explicitly.
     *
     * @see MDC#clear()
     */
    public void clear() {
        // no-op
    }

    private static Map<String, String> currentMdc() {
        return MDC.getCopyOfContextMap();
    }

    private static void setMdc(Map<String, String> mdc) {
        if (mdc == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(mdc);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private static final class Slf4jMdcContext implements Context<Map<String, String>> {
        private final Map<String, String> previous;
        private final AtomicBoolean closed;

        private Slf4jMdcContext(Map<String, String> value) {
            // Capture current MDC as 'previous' and make the given values the 'new current' MDC.
            this.previous = currentMdc();
            setMdc(value);
            this.closed = new AtomicBoolean(false);
        }

        public Map<String, String> getValue() {
            return currentMdc();
        }

        public void close() {
            if (closed.compareAndSet(false, true)) {
                setMdc(previous);
            }
        }

        @Override
        public String toString() {
            Map<String, String> mdc = getValue();
            return closed.get() ? "Slf4jMdcContext{closed}" : "Slf4jMdcContext" + (mdc == null ? "{}" : mdc);
        }
    }
}
