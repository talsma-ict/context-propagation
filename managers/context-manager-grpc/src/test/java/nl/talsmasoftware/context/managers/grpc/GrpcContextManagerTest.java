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
package nl.talsmasoftware.context.managers.grpc;

import io.grpc.Context;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.managers.locale.CurrentLocaleHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("gRPC context manager must be refactored to properly support handling null.")
class GrpcContextManagerTest {
    static final Context.Key<String> TEST_KEY = Context.key("test-key");
    //    static final Locale DUTCH = Locale.of("nl", "NL");
    static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

    GrpcContextManager subject = GrpcContextManager.provider();

    @BeforeEach
    @AfterEach
    void clearAllContexts() {
        subject.clear();
        ContextManager.clearAll();
    }

    @Test
    void testNullContext() {
        try (var ignored = subject.activate(Context.ROOT.withValue(TEST_KEY, "test1"))) {
            assertThat(subject.getActiveContextValue()).isNotNull();
            assertThat(TEST_KEY.get()).isEqualTo("test1");

            try (var ignored2 = subject.activate(null)) {
                assertThat(subject.getActiveContextValue()).isNull();
                assertThat(TEST_KEY.get()).isNull();

                try (var ignored3 = subject.activate(Context.ROOT.withValue(TEST_KEY, "test2"))) {
                    assertThat(subject.getActiveContextValue()).isNotNull();
                    assertThat(TEST_KEY.get()).isEqualTo("test2");
                }

                assertThat(subject.getActiveContextValue()).isNull();
                assertThat(TEST_KEY.get()).isNull();
            }

            assertThat(subject.getActiveContextValue()).isNotNull();
            assertThat(TEST_KEY.get()).isEqualTo("test1");
        }
    }

    @Test
    void testSetGrpcContextValue() {
        String testValue1 = "test-" + UUID.randomUUID();
        String testValue2 = "test2-" + UUID.randomUUID();
        assertThat(TEST_KEY.get()).isNull();
        Context.current().withValue(TEST_KEY, testValue1).run(() -> {
            assertThat(TEST_KEY.get()).isEqualTo(testValue1);
            Context.current().withValue(TEST_KEY, testValue2).run(() -> {
                assertThat(TEST_KEY.get()).isEqualTo(testValue2);
            });
            assertThat(TEST_KEY.get()).isEqualTo(testValue1);
        });
        assertThat(TEST_KEY.get()).isNull();
    }

    @Test
    void testActivateGrpcContext() {
        // given
        String testValue = "test-" + UUID.randomUUID();
        Context randomContext = Context.current().withValue(TEST_KEY, testValue);
        assertThat(TEST_KEY.get()).isNull();

        // when
        try (var ignored = subject.activate(randomContext)) {
            // then
            assertThat(TEST_KEY.get()).isEqualTo(testValue);
        }

        assertThat(TEST_KEY.get()).isNull();
    }

    @Test
    void grpcContextAutomaticallyPropagatesLocaleContext() {
        // given
        CurrentLocaleHolder.set(Locale.GERMANY);
        Context capturedGrpcContext = Context.current();
        CurrentLocaleHolder.set(Locale.FRANCE);

        assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(Locale.FRANCE);
        capturedGrpcContext.run(() -> {
            assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(Locale.GERMANY);
        });

        assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(Locale.FRANCE);
    }

}
