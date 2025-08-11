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
package nl.talsmasoftware.context.timers.opentracing;

import io.opentracing.Span;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.api.ContextTimer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * {@link ContextTimer Context timer} that creates a {@linkplain Span Span}
 * using the {@linkplain GlobalTracer GlobalTracer} for context switches.
 *
 * <p>
 * Individual {@linkplain nl.talsmasoftware.context.api.ContextManager context managers}
 * are <strong>not</strong> traced,
 * only the operations regarding {@linkplain ContextSnapshot} are traced.
 *
 * @author Sjoerd Talsma
 */
public class OpentracingContextTimer implements ContextTimer {
    private static final String SYS_ENABLED = "opentracing.trace.contextmanager";
    private static final String ENV_ENABLED = System.getenv(SYS_ENABLED.toUpperCase().replace('.', '_'));
    private static final String LOG_FIELD_THREAD = "context.thread";

    /**
     * Create the timer for opentracing context propagation.
     *
     * <p>
     * Normally, it is not necessary to instantiate this timer yourself.
     * Providing the {@code context-timer-opentracing} jar file on the classpath
     * should automatically trigger metrics registration using the java ServiceLoader mechanism.
     */
    public OpentracingContextTimer() {
        super();
    }

    @Override
    public void update(Class<?> type, String method, long duration, TimeUnit unit, Throwable error) {
        if (GlobalTracer.isRegistered() && reportContextSwitchesFor(type)) {
            String operationName = type.getSimpleName() + "." + method;
            long finishedMicros = System.currentTimeMillis() * 1000;
            long startTimestampMicros = finishedMicros - unit.toMicros(duration);
            Span span = GlobalTracer.get().buildSpan(operationName).withStartTimestamp(startTimestampMicros).start();
            try {
                Map<String, Object> log = new LinkedHashMap<>();
                if ("capture".equals(method)) {
                    log.put(Fields.EVENT, "New context snapshot captured");
                } else if ("reactivate".equals(method)) {
                    log.put(Fields.EVENT, "Context snapshot reactivated");
                }
                if (error != null) {
                    Tags.ERROR.set(span, true);
                    log.put(Fields.EVENT, "error");
                    log.put(Fields.MESSAGE, error.getMessage());
                    log.put(Fields.ERROR_OBJECT, error);
                }
                log.put(LOG_FIELD_THREAD, Thread.currentThread().getName());
                span.log(log);
            } finally {
                span.finish(finishedMicros);
            }
        }
    }

    private static boolean reportContextSwitchesFor(Class<?> type) {
        boolean enableTracing = false;
        // Only report spans for entire snapshots, not individual context managers.
        // Could be made configurable if somebody ever asks for it.
        if (ContextSnapshot.class.isAssignableFrom(type)) {
            final String prop = System.getProperty(SYS_ENABLED, ENV_ENABLED);
            enableTracing = "1".equals(prop) || "true".equalsIgnoreCase(prop) || "enabled".equalsIgnoreCase(prop);
        }
        return enableTracing;
    }
}
