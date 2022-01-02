/*
 * Copyright 2016-2022 Talsma ICT
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
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
public class DelegatingExecutorServiceTest {

    private static class TestDelegatingExecutorService extends DelegatingExecutorService {
        private TestDelegatingExecutorService(ExecutorService delegate) {
            super(delegate);
        }
    }

    ExecutorService delegate;
    DelegatingExecutorService subject;

    @BeforeEach
    public void setUp() {
        delegate = mock(ExecutorService.class);
        subject = new TestDelegatingExecutorService(delegate);
    }

    @AfterEach
    public void noMoreInteractions() {
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testNullConstructor() {
        TestDelegatingExecutorService tdes = new TestDelegatingExecutorService(null); // No error at construction time.
        try {
            tdes.execute(new Runnable() {
                public void run() {
                    System.out.println("Whoops");
                }
            });
            fail("Informative exception expected.");
        } catch (RuntimeException expected) {
            assertThat(expected, hasToString(containsString("No delegate available for TestDelegatingExecutorService")));
        }
    }

    @Test
    public void testShutdown() {
        subject.shutdown();
        verify(delegate).shutdown();
    }

    @Test
    public void testShutdownNow() {
        List<Runnable> resultObject = emptyList();
        when(delegate.shutdownNow()).thenReturn(resultObject);

        assertThat(subject.shutdownNow(), is(sameInstance(resultObject)));

        verify(delegate).shutdownNow();
    }

    @Test
    public void testIsShutdown() {
        when(delegate.isShutdown()).thenReturn(true);

        assertThat(subject.isShutdown(), is(true));

        verify(delegate).isShutdown();
    }

    @Test
    public void testIsTerminated() {
        when(delegate.isTerminated()).thenReturn(true);

        assertThat(subject.isTerminated(), is(true));

        verify(delegate).isTerminated();
    }

    @Test
    public void testAwaitTermination() throws InterruptedException {
        when(delegate.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertThat(subject.awaitTermination(1234L, MILLISECONDS), is(true));

        verify(delegate).awaitTermination(eq(1234L), eq(MILLISECONDS));
    }

    @Test
    public void testSubmitCallable() {
        Callable callable = mock(Callable.class);
        Future<Object> result = mock(Future.class);
        when(delegate.submit(any(Callable.class))).thenReturn(result);

        assertThat(subject.submit(callable), is(sameInstance((Future) result)));

        verify(delegate).submit(eq(callable));
    }

    @Test
    public void testSubmitRunnable() {
        Runnable runnable = mock(Runnable.class);
        Future<Object> result = mock(Future.class);
        when(delegate.submit(any(Runnable.class), anyObject())).thenReturn(result);

        assertThat(subject.submit(runnable, "yellow"), is(sameInstance((Future) result)));

        verify(delegate).submit(eq(runnable), eq("yellow"));
    }

    @Test
    public void testInvokeAll() throws InterruptedException {
        List<Callable<Object>> calls = emptyList();
        List<Future<Object>> result = emptyList();
        when(delegate.invokeAll(any(Collection.class))).thenReturn(result);

        assertThat(subject.invokeAll(calls), is(equalTo(result)));

        verify(delegate).invokeAll(same(calls));
    }

    @Test
    public void testInvokeAllTimeout() throws InterruptedException {
        List<Callable<Object>> calls = emptyList();
        List<Future<Object>> result = emptyList();
        when(delegate.invokeAll(any(Collection.class), anyLong(), any(TimeUnit.class))).thenReturn(result);

        assertThat(subject.invokeAll(calls, 2364L, MILLISECONDS), is(equalTo(result)));

        verify(delegate).invokeAll(same(calls), eq(2364L), eq(MILLISECONDS));
    }

    @Test
    public void testInvokeAny() throws InterruptedException, ExecutionException {
        List<Callable<Object>> calls = singletonList((Callable<Object>) mock(Callable.class));
        Object result = new Object();
        when(delegate.invokeAny(any(Collection.class))).thenReturn(result);

        assertThat(subject.invokeAny(calls), is(sameInstance(result)));

        verify(delegate).invokeAny(eq(calls));
    }

    @Test
    public void testInvokeAnyTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        List<Callable<Object>> calls = singletonList((Callable<Object>) mock(Callable.class));
        Object result = new Object();
        when(delegate.invokeAny(any(Collection.class), anyLong(), any(TimeUnit.class))).thenReturn(result);

        assertThat(subject.invokeAny(calls, 2873L, MILLISECONDS), is(sameInstance(result)));

        verify(delegate).invokeAny(eq(calls), eq(2873L), eq(MILLISECONDS));
    }

    @Test
    public void testExecute() {
        Runnable runnable = mock(Runnable.class);
        subject.execute(runnable);
        verify(delegate).execute(same(runnable));
    }

    @Test
    public void testHashCode() {
        int hash = delegate.hashCode();
        assertThat(subject.hashCode(), is(hash));
    }

    @Test
    public void testEquals() {
        assertThat(subject, is(equalTo(subject)));
        assertThat(subject, is(equalTo((Object) new TestDelegatingExecutorService(delegate))));
        assertThat(subject, is(not(equalTo((Object) new TestDelegatingExecutorService(mock(ExecutorService.class))))));
    }

    @Test
    public void testToString() {
        assertThat(subject, hasToString("TestDelegatingExecutorService{" + delegate + "}"));
    }

}
