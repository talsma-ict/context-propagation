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
package nl.talsmasoftware.context.core.delegation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Pretty silly test that verifies that all methods are actually passed on to a delegate.
 *
 * @author Sjoerd Talsma
 */
class DelegatingExecutorServiceTest {

    static class TestDelegatingExecutorService extends DelegatingExecutorService {
        TestDelegatingExecutorService(ExecutorService delegate) {
            super(delegate);
        }
    }

    ExecutorService delegate;
    DelegatingExecutorService subject;

    @BeforeEach
    void setUp() {
        delegate = mock(ExecutorService.class);
        subject = new TestDelegatingExecutorService(delegate);
    }

    @AfterEach
    void noMoreInteractions() {
        verifyNoMoreInteractions(delegate);
    }

    @Test
    void testShutdown() {
        subject.shutdown();
        verify(delegate).shutdown();
    }

    @Test
    void testShutdownNow() {
        List<Runnable> resultObject = emptyList();
        when(delegate.shutdownNow()).thenReturn(resultObject);

        assertThat(subject.shutdownNow()).isSameAs(resultObject);

        verify(delegate).shutdownNow();
    }

    @Test
    void testIsShutdown() {
        when(delegate.isShutdown()).thenReturn(true);

        assertThat(subject.isShutdown()).isTrue();

        verify(delegate).isShutdown();
    }

    @Test
    void testIsTerminated() {
        when(delegate.isTerminated()).thenReturn(true);

        assertThat(subject.isTerminated()).isTrue();

        verify(delegate).isTerminated();
    }

    @Test
    void testAwaitTermination() throws InterruptedException {
        when(delegate.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertThat(subject.awaitTermination(1234L, MILLISECONDS)).isTrue();

        verify(delegate).awaitTermination(1234L, MILLISECONDS);
    }

    @Test
    void testSubmitCallable() {
        Callable<?> callable = mock(Callable.class);
        Future<Object> result = mock(Future.class);
        when(delegate.submit(any(Callable.class))).thenReturn(result);

        assertThat(subject.submit(callable)).isSameAs(result);

        verify(delegate).submit(callable);
    }

    @Test
    void testSubmitRunnable() {
        Runnable runnable = mock(Runnable.class);
        Future<Object> result = mock(Future.class);
        when(delegate.submit(any(Runnable.class), any())).thenReturn(result);

        assertThat(subject.submit(runnable, "yellow")).isSameAs(result);

        verify(delegate).submit(runnable, "yellow");
    }

    @Test
    void testInvokeAll() throws InterruptedException {
        List<Callable<Object>> calls = emptyList();
        List<Future<Object>> result = emptyList();
        when(delegate.invokeAll(any(Collection.class))).thenReturn(result);

        assertThat(subject.invokeAll(calls)).isEqualTo(result);

        verify(delegate).invokeAll(calls);
    }

    @Test
    void testInvokeAllTimeout() throws InterruptedException {
        List<Callable<Object>> calls = emptyList();
        List<Future<Object>> result = emptyList();
        when(delegate.invokeAll(any(Collection.class), anyLong(), any(TimeUnit.class))).thenReturn(result);

        assertThat(subject.invokeAll(calls, 2364L, MILLISECONDS)).isEqualTo(result);

        verify(delegate).invokeAll(calls, 2364L, MILLISECONDS);
    }

    @Test
    void testInvokeAny() throws InterruptedException, ExecutionException {
        List<Callable<Object>> calls = singletonList((Callable<Object>) mock(Callable.class));
        Object result = new Object();
        when(delegate.invokeAny(any(Collection.class))).thenReturn(result);

        assertThat(subject.invokeAny(calls)).isSameAs(result);

        verify(delegate).invokeAny(calls);
    }

    @Test
    void testInvokeAnyTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        List<Callable<Object>> calls = singletonList((Callable<Object>) mock(Callable.class));
        Object result = new Object();
        when(delegate.invokeAny(any(Collection.class), anyLong(), any(TimeUnit.class))).thenReturn(result);

        assertThat(subject.invokeAny(calls, 2873L, MILLISECONDS)).isSameAs(result);

        verify(delegate).invokeAny(calls, 2873L, MILLISECONDS);
    }

    @Test
    void testExecute() {
        Runnable runnable = mock(Runnable.class);
        subject.execute(runnable);
        verify(delegate).execute(same(runnable));
    }

    @Test
    void testHashCode() {
        int hash = delegate.hashCode();
        assertThat(subject.hashCode()).isEqualTo(hash);
    }

    @Test
    void testEquals() {
        assertThat(subject)
                .isEqualTo(subject)
                .isEqualTo(new TestDelegatingExecutorService(delegate))
                .isNotEqualTo(new TestDelegatingExecutorService(mock(ExecutorService.class)));
    }

    @Test
    void testToString() {
        assertThat(subject).hasToString("TestDelegatingExecutorService{" + delegate + "}");
    }

}
