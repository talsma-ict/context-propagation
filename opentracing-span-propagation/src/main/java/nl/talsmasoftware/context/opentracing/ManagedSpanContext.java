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

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.SpanManager;
import io.opentracing.contrib.spanmanager.SpanManager.ManagedSpan;
import nl.talsmasoftware.context.Context;

import java.io.Closeable;
import java.io.IOException;

/**
 * A {@link Context} implementation based on a {@link ManagedSpan} from a {@link SpanManager}.
 *
 * @author Sjoerd Talsma
 */
final class ManagedSpanContext implements Context<Span> {

    private final Span value;
    private final Closeable closeable;

    ManagedSpanContext(Span span) {
        this.value = span;
        this.closeable = null;
    }

    ManagedSpanContext(ManagedSpan managedSpan) {
        if (managedSpan == null) throw new NullPointerException("ManagedSpan is <null>.");
        this.value = managedSpan.getSpan();
        this.closeable = managedSpan;
    }

    @Override
    public Span getValue() {
        return value;
    }

    @Override
    public void close() {
        if (closeable != null) try {
            closeable.close();
        } catch (IOException ioe) {
            throw new RuntimeException("I/O exception closing " + this + ": " + ioe.getMessage(), ioe);
        }
    }

    public String toString() {
        return "ManagedSpanContext{" + value + '}';
    }

}
