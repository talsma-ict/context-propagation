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
package nl.talsmasoftware.context.opentracing;

import io.opentracing.ActiveSpan;
import io.opentracing.util.GlobalTracer;
import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManager;

/**
 * {@link ContextManager} implementation that manages OpenTracing {@link ActiveSpan} continuations.
 * <p>
 * It delegates management of the {@link ActiveSpan} to the {@link GlobalTracer} and
 * {@link ActiveSpan.Continuation#activate() activates} the specified {@link ActiveSpan.Continuation}
 * when a {@link #initializeNewContext(ActiveSpan.Continuation) new context must be initialized}.
 * The most common case where this happens is
 * at {@link nl.talsmasoftware.context.ContextSnapshot#reactivate() snapshot reactivation}.
 * <p>
 * To comply with the {@link ActiveSpan#capture()} contract, each {@link Context#getValue()} result
 * <strong>must</strong> be {@link ActiveSpan.Continuation#activate() activated} and
 * {@link ActiveSpan#deactivate() deactivated}to prevent leaving the
 * active {@link io.opentracing.Span Span} un-finished.<br>
 * Using a {@link nl.talsmasoftware.context.executors.ContextAwareExecutorService ContextAwareExecutorService}
 * will make sure each created {@link nl.talsmasoftware.context.ContextSnapshot context snapshot}
 * gets reactivated and closed properly.
 *
 * @author Sjoerd Talsma
 */
public class ActiveSpanManager implements ContextManager<ActiveSpan.Continuation> {

    @Override
    public Context<ActiveSpan.Continuation> getActiveContext() {
        return new Context<ActiveSpan.Continuation>() {
            final ActiveSpan activeSpan = GlobalTracer.get().activeSpan();

            @Override
            public ActiveSpan.Continuation getValue() {
                return activeSpan == null ? null : activeSpan.capture();
            }

            @Override
            public void close() {
                if (activeSpan != null) activeSpan.close();
            }
        };
    }

    @Override
    public Context<ActiveSpan.Continuation> initializeNewContext(final ActiveSpan.Continuation continuation) {
        return new Context<ActiveSpan.Continuation>() {
            final ActiveSpan activated = continuation == null ? null : continuation.activate();

            @Override
            public ActiveSpan.Continuation getValue() {
                return continuation;
            }

            @Override
            public void close() {
                if (activated != null) activated.deactivate();
            }
        };
    }

}
