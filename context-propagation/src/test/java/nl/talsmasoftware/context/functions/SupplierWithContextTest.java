/*
 * Copyright 2016-2019 Talsma ICT
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
package nl.talsmasoftware.context.functions;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.DummyContextManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;

/**
 * @author Sjoerd Talsma
 */
public class SupplierWithContextTest {

    private ExecutorService unawareThreadpool;
    private ContextSnapshot snapshot;
    private Context<Void> context;

    @Before
    @After
    public void clearDummyContext() {
        DummyContextManager.clear();
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        unawareThreadpool = Executors.newCachedThreadPool();
        snapshot = mock(ContextSnapshot.class);
        context = mock(Context.class);
    }

    @After
    public void tearDown() throws InterruptedException {
        verifyNoMoreInteractions(snapshot, context);
        unawareThreadpool.shutdown();
        unawareThreadpool.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void testGetNull() {
        assertThat(new SupplierWithContext<>(snapshot, () -> null).get(), is(nullValue()));
        verify(snapshot).reactivate();
    }

    @Test
    public void testGetWithSnapshotConsumer() throws ExecutionException, InterruptedException {
        DummyContextManager.setCurrentValue("Old value");
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];

        Supplier<Optional<String>> supplier = new SupplierWithContext<>(snapshot, () -> {
            try {
                return DummyContextManager.currentValue();
            } finally {
                DummyContextManager.setCurrentValue("New value");
            }
        }, s -> snapshotHolder[0] = s);
        
        Future<Optional<String>> future = unawareThreadpool.submit(supplier::get);
        assertThat(future.get(), is(Optional.empty()));

        verify(snapshot).reactivate();
        assertThat(DummyContextManager.currentValue(), is(Optional.of("Old value")));
        try (Context<Void> reactivation = snapshotHolder[0].reactivate()) {
            assertThat(DummyContextManager.currentValue(), is(Optional.of("New value")));
        }
    }

}
