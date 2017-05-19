/*
 * Copyright 2016-2017 Talsma ICT
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
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager to propagate MDC content from one thread to another.
 *
 * @author Sjoerd Talsma
 */
public class MdcManager implements ContextManager<Map<String, String>> {

    public Context<Map<String, String>> initializeNewContext(final Map<String, String> value) {
        // Capture current MDC as 'previous' and make value the new current MCC.
        final Map<String, String> previous = MDC.getCopyOfContextMap();
        if (value == null) MDC.clear();
        else MDC.setContextMap(value);
        return new MdcContext(previous, value, false);
    }

    public Context<Map<String, String>> getActiveContext() {
        // Return fresh context that is 'already-closed' without a previous mdc.
        return new MdcContext(null, MDC.getCopyOfContextMap(), true);
    }

    private static final class MdcContext implements Context<Map<String, String>> {
        private final Map<String, String> previous, value;
        private final AtomicBoolean closed;

        private MdcContext(Map<String, String> previous, Map<String, String> value, boolean closed) {
            this.previous = previous;
            this.value = value;
            this.closed = new AtomicBoolean(closed);
        }

        public Map<String, String> getValue() {
            return value;
        }

        public void close() {
            if (closed.compareAndSet(false, true)) {
                if (previous == null) MDC.clear();
                else MDC.setContextMap(previous);
            }
        }

        @Override
        public String toString() {
            return closed.get() ? "MdcContext{closed}" : "MdcContext" + (value == null ? "{}" : value);
        }
    }
}
