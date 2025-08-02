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
package nl.talsmasoftware.context.delegation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Simple unit test to verify the overridable mapping method does get called for all task-scheduling methods by the
 * superclass.
 *
 * @author Sjoerd Talsma
 */
public class MappingExecutorServiceTest {

    ExecutorService delegate;
    TestMappingExecutorService subject;

    @BeforeEach
    public void setUp() {
        delegate = mock(ExecutorService.class);
        subject = new TestMappingExecutorService(delegate);
    }

    @AfterEach
    public void noMoreInteractions() {
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testMapRunnable() {
        final AtomicInteger runCounter = new AtomicInteger(0);
        Runnable mapped = subject.wrap(new Runnable() {
            public void run() {
                runCounter.incrementAndGet();
            }
        });

        assertThat(runCounter.get(), is(0));
        assertThat(subject.mapCount.get(), is(1));
        assertThat(subject.callCount.get(), is(0));
        mapped.run();

        assertThat(runCounter.get(), is(1));
        assertThat(subject.mapCount.get(), is(1));
        assertThat(subject.callCount.get(), is(1));
    }

    @Test
    public void testSubmitCallable() {
        Callable<Object> call = mock(Callable.class);
        Future<Object> result = mock(Future.class);
        when(delegate.submit(any(Callable.class))).thenReturn(result);

        assertThat(subject.submit(call), is(sameInstance(result)));
        assertThat(subject.mapCount.get(), is(1));

        verify(delegate).submit(any(Callable.class));
    }

    @Test
    public void testSubmitRunnable() {
        Runnable runnable = mock(Runnable.class);
        Future result = mock(Future.class);
        when(delegate.submit(any(Runnable.class))).thenReturn(result);

        assertThat(subject.submit(runnable), is(sameInstance((Object) result)));
        assertThat(subject.mapCount.get(), is(1));

        verify(delegate).submit(any(Runnable.class));
    }

    @Test
    public void testSubmitRunnableResult() {
        Runnable runnable = mock(Runnable.class);
        Future<Object> result = mock(Future.class);
        when(delegate.submit(any(Runnable.class), anyObject())).thenReturn(result);

        assertThat(subject.submit(runnable, new Object()), is(sameInstance(result)));
        assertThat(subject.mapCount.get(), is(1));

        verify(delegate).submit(any(Runnable.class), anyObject());
    }

    private static Collection<Callable<Object>> calls(int size) {
        Collection<Callable<Object>> calls = new ArrayList<Callable<Object>>();
        for (int i = 0; i < size; i++) calls.add(mock(Callable.class));
        return calls;
    }

    private static List<Future<Object>> futures(int size) {
        List<Future<Object>> futures = new ArrayList<Future<Object>>();
        for (int i = 0; i < size; i++) futures.add(mock(Future.class));
        return futures;
    }

    @Test
    public void testInvokeAll() throws InterruptedException {
        List<Future<Object>> result = futures(3);
        when(delegate.invokeAll(anyCollection())).thenReturn(result);

        assertThat(subject.invokeAll(calls(3)), is(equalTo(result)));
        assertThat(subject.mapCount.get(), is(3));

        verify(delegate).invokeAll((Collection<Callable<Object>>) argThat(hasSize(3)));
    }

    @Test
    public void testInvokeAllTimeout() throws InterruptedException {
        List<Future<Object>> result = futures(5);
        when(delegate.invokeAll(anyCollection(), anyLong(), any(TimeUnit.class))).thenReturn(result);

        assertThat(subject.invokeAll(calls(5), 1274L, MILLISECONDS), is(equalTo(result)));
        assertThat(subject.mapCount.get(), is(5));

        verify(delegate).invokeAll((Collection<Callable<Object>>) argThat(hasSize(5)), eq(1274L), eq(MILLISECONDS));
    }

    @Test
    public void testInvokeAny() throws InterruptedException, ExecutionException {
        Object result = new Object();
        when(delegate.invokeAny((Collection<Callable<Object>>) anyObject())).thenReturn(result);

        assertThat(subject.invokeAny(calls(2)), is(sameInstance(result)));
        assertThat(subject.mapCount.get(), is(2));

        verify(delegate).invokeAny((Collection<Callable<Object>>) argThat(hasSize(2)));
    }

    @Test
    public void testInvokeAnyTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        Object result = new Object();
        when(delegate.invokeAny((Collection<Callable<Object>>) anyObject(), anyLong(), any(TimeUnit.class))).thenReturn(result);

        assertThat(subject.invokeAny(calls(7), 3276L, MILLISECONDS), is(sameInstance(result)));
        assertThat(subject.mapCount.get(), is(7));

        verify(delegate).invokeAny((Collection<Callable<Object>>) argThat(hasSize(7)), eq(3276L), eq(MILLISECONDS));
    }

    @Test
    public void testExecute() {
        subject.execute(mock(Runnable.class));
        assertThat(subject.mapCount.get(), is(1));
        verify(delegate).execute(any(Runnable.class));
    }

    static class TestMappingExecutorService extends CallMappingExecutorService {
        final AtomicInteger mapCount = new AtomicInteger(0);
        final AtomicInteger callCount = new AtomicInteger(0);

        TestMappingExecutorService(ExecutorService delegate) {
            super(delegate);
        }

        protected <V> Callable<V> map(final Callable<V> callable) {
            mapCount.incrementAndGet();
            return new Callable<V>() {
                public V call() throws Exception {
                    callCount.incrementAndGet();
                    return callable.call();
                }
            };
        }
    }


}
