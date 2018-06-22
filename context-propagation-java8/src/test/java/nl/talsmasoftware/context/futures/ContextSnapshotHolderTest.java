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
package nl.talsmasoftware.context.futures;

import nl.talsmasoftware.context.ContextSnapshot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * @author Sjoerd Talsma
 */
public class ContextSnapshotHolderTest {
    private ExecutorService unawareThreadpool;

    @Before
    public void setUp() {
        unawareThreadpool = Executors.newCachedThreadPool();
    }

    @After
    public void tearDown() throws InterruptedException {
        unawareThreadpool.shutdown();
        unawareThreadpool.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void testSetGet() {
        ContextSnapshot snapshot = mock(ContextSnapshot.class);
        ContextSnapshotHolder holder = new ContextSnapshotHolder();

        holder.accept(snapshot);
        assertThat(holder.get(), is(sameInstance(snapshot)));
    }

    @Test
    public void testInterruption() throws InterruptedException {
        ContextSnapshot snapshot = mock(ContextSnapshot.class);
        ContextSnapshotHolder holder = new ContextSnapshotHolder();

        Future<ContextSnapshot> future = unawareThreadpool.submit(holder::get);
        unawareThreadpool.shutdownNow();
        unawareThreadpool.awaitTermination(5, TimeUnit.SECONDS);

        try {
            future.get();
            fail("Exception expected");
        } catch (ExecutionException expected) {
            assertThat(expected.getCause(), hasToString(containsString("Interrupted waiting for context snapshot")));
        }
    }

}
