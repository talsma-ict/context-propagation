/*
 * Copyright 2016-2026 Talsma ICT
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
package nl.talsmasoftware.context.managers.spring.security;

import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.core.concurrent.ContextAwareExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Closeable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for the {@link SpringSecurityContextManager}.
 *
 * @author Sjoerd Talsma
 */
public class SpringSecurityContextManagerTest {

    ExecutorService threadpool;

    @BeforeEach
    public void setupThreadpool() {
        threadpool = ContextAwareExecutorService.wrap(Executors.newCachedThreadPool());
    }

    @AfterEach
    public void shutdownThreadpool() {
        threadpool.shutdown();
        threadpool = null;
    }

    @BeforeEach
    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static final Callable<Authentication> GET_AUTHENTICATION = new Callable<Authentication>() {
        public Authentication call() {
            SecurityContext context = SecurityContextHolder.getContext();
            return context == null ? null : context.getAuthentication();
        }
    };

    private static void setAuthentication(String name) {
        SecurityContext newContext = SecurityContextHolder.createEmptyContext();
        newContext.setAuthentication(name == null ? null : new TestAuthentication(name));
        SecurityContextHolder.setContext(newContext);
    }

    @Test
    public void testWithoutAnyAuthentication() {
        assertThat(SpringSecurityContextManager.provider().getActiveContextValue()).isNull();
    }

    @Test
    public void testAuthenticationPropagation() throws ExecutionException, InterruptedException {
        setAuthentication("Mr. Bean");
        Future<Authentication> itemValue = threadpool.submit(GET_AUTHENTICATION);

        SecurityContextHolder.clearContext();
        assertThat(itemValue.get().toString()).contains("Mr. Bean");
    }

    @Test
    public void testAuthenticationReactivation() throws Exception {
        setAuthentication("Vincent Vega");

        ContextSnapshot snapshot = ContextSnapshot.capture();
        assertThat(GET_AUTHENTICATION.call().toString()).withFailMessage("New snapshot shouldn't manipulate context.")
                .contains("Vincent Vega");

        setAuthentication("Jules Winnfield");
        assertThat(GET_AUTHENTICATION.call().toString()).withFailMessage("Sanity check: Context changed?")
                .contains("Jules Winnfield");

        Closeable reactivation = snapshot.reactivate();
        assertThat(GET_AUTHENTICATION.call().toString()).withFailMessage("Context changed by reactivation")
                .contains("Vincent Vega");

        reactivation.close();
        assertThat(GET_AUTHENTICATION.call().toString()).withFailMessage("Context restored?")
                .contains("Jules Winnfield");
    }

    @Test
    public void testClearableImplementation() {
        setAuthentication("Vincent Vega");
        assertThat(SpringSecurityContextManager.provider().getActiveContextValue().getName()).isEqualTo("Vincent Vega");

        ContextManager.clearAll();
        assertThat(SpringSecurityContextManager.provider().getActiveContextValue()).isNull();
    }

}
