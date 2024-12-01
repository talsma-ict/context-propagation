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
package nl.talsmasoftware.context.managers.opentelemetry;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class OpenTelemetryContextManagerTest {
    @RegisterExtension
    static final OpenTelemetryExtension TELEMETRY = OpenTelemetryExtension.create();

    static OpenTelemetryContextManager subject = OpenTelemetryContextManager.provider();

    @Test
    void getActiveContextValue_delegatesToContext_current() {
        // prepare
        ContextKey<Object> dummyKey = ContextKey.named("dummy");
        String dummyValue = UUID.randomUUID().toString();

        try (Scope scope = Context.current().with(dummyKey, dummyValue).makeCurrent()) {
            // execute
            Context activeCtx = subject.getActiveContextValue();

            // verify
            assertThat(activeCtx.get(dummyKey)).isEqualTo(dummyValue);
        }
    }

    @Test
    void initializeNewContext_makes_specified_Context_current() {
        // prepare
        ContextKey<Object> dummyKey = ContextKey.named("dummy");
        String dummyValue = UUID.randomUUID().toString();
        Context newContext = Context.current().with(dummyKey, dummyValue);
        assertThat(Context.current().get(dummyKey)).isNull();

        // execute
        try (nl.talsmasoftware.context.api.Context<Context> context = subject.initializeNewContext(newContext)) {

            // verify
            assertThat(Context.current().get(dummyKey)).isEqualTo(dummyValue);
            assertThat(context).isNotNull();
            assertThat(subject.getActiveContextValue()).isSameAs(newContext);
        }

        assertThat(Context.current().get(dummyKey)).isNull();
    }

    @Test
    void clear_does_nothing_but_also_does_not_throw_exception() {
        // prepare
        ContextKey<Object> dummyKey = ContextKey.named("dummy");
        String dummyValue = UUID.randomUUID().toString();
        try (Scope scope = Context.current().with(dummyKey, dummyValue).makeCurrent()) {

            // execute
            assertDoesNotThrow(subject::clear);

            // verify
            assertThat(Context.current().get(dummyKey)).isEqualTo(dummyValue);
        }
    }

    @Test
    void toString_returns_simple_classname() {
        assertThat(subject).hasToString("OpenTelemetryContextManager");
    }
}
