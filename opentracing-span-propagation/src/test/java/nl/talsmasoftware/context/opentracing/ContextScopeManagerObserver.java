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
package nl.talsmasoftware.context.opentracing;

import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import nl.talsmasoftware.context.ContextManager;
import nl.talsmasoftware.context.observer.ContextObserver;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.synchronizedList;

public class ContextScopeManagerObserver implements ContextObserver<Span> {
    static final List<Event> observed = synchronizedList(new ArrayList<Event>());

    @Override
    public Class<? extends ContextManager<Span>> getObservedContextManager() {
        return ContextScopeManager.class;
    }

    @Override
    public void onActivate(Span activatedContextValue, Span previousContextValue) {
        observed.add(new Event(Event.Type.ACTIVATE, activatedContextValue));
    }

    @Override
    public void onDeactivate(Span deactivatedContextValue, Span restoredContextValue) {
        observed.add(new Event(Event.Type.DEACTIVATE, deactivatedContextValue));
    }

    static class Event {
        enum Type {ACTIVATE, DEACTIVATE}

        final Thread thread;
        final Type type;
        final Span value;

        Event(Type type, Span value) {
            this.thread = Thread.currentThread();
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Event{" + type + ", thread=" + thread.getName() + ", span=" + value + '}';
        }
    }

    static class EventMatcher extends BaseMatcher<Event> {
        Thread inThread;
        Event.Type type;
        Matcher<MockSpan> spanMatcher;

        private EventMatcher(Event.Type type, Matcher<MockSpan> spanMatcher) {
            this.type = type;
            this.spanMatcher = spanMatcher;
        }

        static EventMatcher activated(Matcher<MockSpan> span) {
            return new EventMatcher(Event.Type.ACTIVATE, span);
        }

        static EventMatcher deactivated(Matcher<MockSpan> span) {
            return new EventMatcher(Event.Type.DEACTIVATE, span);
        }

        EventMatcher inThread(Thread thread) {
            EventMatcher copy = new EventMatcher(type, spanMatcher);
            copy.inThread = thread;
            return copy;
        }

        @Override
        public boolean matches(Object actual) {
            if (!(actual instanceof Event)) return actual == null;
            Event actualEv = (Event) actual;
            return (inThread == null || inThread.equals(actualEv.thread))
                    && type.equals(actualEv.type)
                    && spanMatcher.matches(actualEv.value);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Event ");
            if (inThread != null) description.appendText("in thread ").appendText(inThread.getName());
            description.appendValue(type).appendText(" ");
            spanMatcher.describeTo(description);
        }
    }
}
