/*
 * Copyright 2016-2019 Talsma ICT
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
package nl.talsmasoftware.context.mdc;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManager;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.clearable.Clearable;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager to propagate the {@link MDC} content from one thread to another.
 * <p>
 * As {@link MDC} already manages its own threadlocal state,
 * getting the active context is 100% delegated to the MDC.<br>
 * This means that closing the resulting context from {@link #getActiveContext()} will have no side-effects,
 * as it is not ours to manage.
 * <p>
 * Closing a context returned form {@link #initializeNewContext(Map)} <strong>will</strong> reset the MDC
 * to the values it had before the context was created.<br>
 * This means that closing nested contexts out-of-order will probably result in an undesirable state.<br>
 * It is therefore strongly advised to use Java's {@code try-with-resources} mechanism to ensure proper
 * closing when nesting new MDC contexts.
 *
 * @author Sjoerd Talsma
 */
public class MdcManager implements ContextManager<Map<String, String>> {

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
        // Capture current MDC as 'previous' and make the given values the 'new current' MDC.
        final Map<String, String> previous = MDC.getCopyOfContextMap();
        if (mdcValues == null) MDC.clear();
        else MDC.setContextMap(mdcValues);
        return new MdcContext(previous, mdcValues, false);
    }

    /**
     * Returns the active MDC values from the current thread.
     * <p>
     * <strong>Please note:</strong> <em>Because these values are managed by MDC itself and not by us,
     * closing the resulting context has no effect.</em>
     *
     * @return Context containing the active MDC values.
     */
    public Context<Map<String, String>> getActiveContext() {
        // Return fresh context that is 'already-closed'. Therefore it doesn't need a previous mdc.
        return new MdcContext(null, MDC.getCopyOfContextMap(), true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private static final class MdcContext implements Context<Map<String, String>>, Clearable {
        private final Map<String, String> previous, value;
        private final AtomicBoolean closed;

        private MdcContext(Map<String, String> previous, Map<String, String> value, boolean closed) {
            this.previous = previous;
            this.value = value;
            this.closed = new AtomicBoolean(closed);
            ContextManagers.onActivate(MdcManager.class, value, previous);
        }

        public Map<String, String> getValue() {
            return value;
        }

        public void close() {
            if (closed.compareAndSet(false, true)) {
                if (previous == null) MDC.clear();
                else MDC.setContextMap(previous);
                ContextManagers.onDeactivate(MdcManager.class, value, previous);
            }
        }

        public void clear() {
            close();
        }

        @Override
        public String toString() {
            return closed.get() ? "MdcContext{closed}" : "MdcContext" + (value == null ? "{}" : value);
        }

    }
}
