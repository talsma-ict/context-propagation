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
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.managers.locale.CurrentLocaleHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.OPTIONAL;

class GrpcContextManagerTest {
    static final Context.Key<String> TEST_KEY = Context.key("test-key");
    static final Locale DUTCH = Locale.of("nl", "NL");
    static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

    @BeforeEach
    @AfterEach
    void clearAllContexts() {
        ContextManager.clearAll();
    }

    @Test
    void grpcContextIsPropagatedToBackgroundThread() {
        String testValue = "test-" + UUID.randomUUID();
        Context context = Context.current().withValue(TEST_KEY, testValue).attach();
        try {

            assertThat(THREAD_POOL.submit(() -> TEST_KEY.get()))
                    .as("Test value from gRPC context in plain thread")
                    .succeedsWithin(1, TimeUnit.SECONDS)
                    .isNull();

            ContextSnapshot snapshot = ContextSnapshot.capture();
            assertThat(THREAD_POOL.submit(snapshot.wrap(() -> TEST_KEY.get())))
                    .as("Test value from gRPC context in thread with snapshot")
                    .succeedsWithin(1, TimeUnit.SECONDS)
                    .isEqualTo(testValue);

        } finally {
            context.detach(Context.current());
        }
    }

    @Test
    void contextManagersArePropagatedWithGrpcContexg() {
        CurrentLocaleHolder.set(DUTCH);

        assertThat(THREAD_POOL.submit(CurrentLocaleHolder::get))
                .as("Current Locale in plain thread")
                .succeedsWithin(1, TimeUnit.SECONDS)
                .asInstanceOf(OPTIONAL)
                .isEmpty();

        assertThat(THREAD_POOL.submit(Context.current().wrap(CurrentLocaleHolder::get)))
                .as("Current Locale in thread with gRPC context")
                .succeedsWithin(1, TimeUnit.SECONDS)
                .asInstanceOf(OPTIONAL)
                .contains(DUTCH);

        assertThat(THREAD_POOL.submit(Context.ROOT.wrap(CurrentLocaleHolder::get)))
                .as("Current Locale in plain thread")
                .succeedsWithin(1, TimeUnit.SECONDS)
                .asInstanceOf(OPTIONAL)
                .isEmpty();

    }

}
