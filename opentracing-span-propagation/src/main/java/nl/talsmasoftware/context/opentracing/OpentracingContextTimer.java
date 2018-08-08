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
package nl.talsmasoftware.context.opentracing;

import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.timing.ContextTimer;

import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonMap;

/**
 * This {@link ContextTimer} uses the {@code Opentracing API} to report the timing statistics of the context switches
 * that occurred.
 * <p>
 * This class does not really <em>support</em> Opentracing as that it <em>uses</em> opentracing to report
 * the timing metrics of this library.
 *
 * @author Sjoerd Talsma
 */
public class OpentracingContextTimer implements ContextTimer {
    private static final String SYS_DISABLED = "opentracing.contexttimer.disabled";
    private static final String ENV_DISABLED = SYS_DISABLED.toUpperCase().replace('.', '_');
    private static final String LOG_FIELD_THREAD = "context.thread";

    @Override
    public void update(Class<?> type, String method, long duration, TimeUnit unit) {
        if (GlobalTracer.isRegistered() && reportContextSwitchesFor(type)) {
            String operationName = type.getSimpleName() + "." + method;
            long finishedMicros = System.currentTimeMillis() * 1000;
            long startTimestampMicros = finishedMicros - unit.toMicros(duration);
            Span span = GlobalTracer.get().buildSpan(operationName).withStartTimestamp(startTimestampMicros).start();
            try {
                if ("createContextSnapshot".equals(method)) {
                    span.log("New context snapshot created");
                    span.log(singletonMap(LOG_FIELD_THREAD, Thread.currentThread().getName()));
                } else if ("reactivate".equals(method)) {
                    span.log("Context snapshot reactivated");
                    span.log(singletonMap(LOG_FIELD_THREAD, Thread.currentThread().getName()));
                }
            } finally {
                span.finish(finishedMicros);
            }
        }
    }

    private static boolean reportContextSwitchesFor(Class<?> type) {
        final String disabled = System.getProperty(SYS_DISABLED, System.getenv(ENV_DISABLED));
        if ("1".equals(disabled) || "true".equalsIgnoreCase(disabled)) return false;

        // Only report spans for entire snapshots, not individual context managers
        // Could be made configurable if somebody ever asks for it..
        return ContextManagers.class.equals(type) || ContextSnapshot.class.equals(type);
    }
}
