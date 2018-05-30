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
package nl.talsmasoftware.context.springsecurity;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.executors.ContextAwareExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit test for the {@link SpringSecurityContextManager}.
 *
 * @author Sjoerd Talsma
 */
public class SpringSecurityContextManagerTest {

    ExecutorService threadpool;

    @Before
    public void setupThreadpool() {
        threadpool = new ContextAwareExecutorService(Executors.newCachedThreadPool());
    }

    @After
    public void shutdownThreadpool() {
        threadpool.shutdown();
        threadpool = null;
    }

    @Before
    @After
    public void clearMDC() {
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
        assertThat(new SpringSecurityContextManager().getActiveContext().getValue(), is(nullValue()));
    }

    @Test
    public void testAuthenticationPropagation() throws ExecutionException, InterruptedException {
        setAuthentication("Mr. Bean");
        Future<Authentication> itemValue = threadpool.submit(GET_AUTHENTICATION);

        SecurityContextHolder.clearContext();
        assertThat(itemValue.get(), hasToString(containsString("Mr. Bean")));
    }

    @Test
    public void testAuthenticationReactivation() throws Exception {
        setAuthentication("Vincent Vega");

        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        assertThat("New snapshot shouldn't manipulate context.", GET_AUTHENTICATION.call(),
                hasToString(containsString("Vincent Vega")));

        setAuthentication("Jules Winnfield");
        assertThat("Sanity check: Context changed?", GET_AUTHENTICATION.call(),
                hasToString(containsString("Jules Winnfield")));

        Context<Void> reactivation = snapshot.reactivate();
        assertThat("Context changed by reactivation", GET_AUTHENTICATION.call(),
                hasToString(containsString("Vincent Vega")));

        reactivation.close();
        assertThat("Context restored?", GET_AUTHENTICATION.call(),
                hasToString(containsString("Jules Winnfield")));
    }

    @Test
    public void testClosingCurrentAuthenticationContext() {

    }

}
