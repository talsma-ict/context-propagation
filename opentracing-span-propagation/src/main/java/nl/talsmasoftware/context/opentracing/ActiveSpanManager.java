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
 * @author Sjoerd Talsma
 */
public class ActiveSpanManager implements ContextManager<ActiveSpan.Continuation> {

    @Override
    public Context<ActiveSpan.Continuation> getActiveContext() {
        final ActiveSpan activeSpan = GlobalTracer.get().activeSpan();
        return new Context<ActiveSpan.Continuation>() {
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
        final ActiveSpan activated = continuation.activate();

        return new Context<ActiveSpan.Continuation>() {
            @Override
            public ActiveSpan.Continuation getValue() {
                return activated != null ? activated.capture() : null;
            }

            @Override
            public void close() {
                if (activated != null) activated.deactivate();
            }
        };
    }

}
