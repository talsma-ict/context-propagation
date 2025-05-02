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
package nl.talsmasoftware.context.core.delegation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Pretty silly test that verifies that all methods are actually passed on to a delegate.
 *
 * @author Sjoerd Talsma
 */
public class DelegatingFutureTest {

    private static class TestDelegatingFuture extends DelegatingFuture<Object> {
        private TestDelegatingFuture(Future<Object> delegate) {
            super(delegate);
        }
    }

    Future<Object> delegate;
    DelegatingFuture<Object> subject;

    @BeforeEach
    public void setUp() {
        delegate = mock(Future.class);
        subject = new TestDelegatingFuture(delegate);
    }

    @AfterEach
    public void noMoreInteractions() {
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testCancel() {
        when(delegate.cancel(anyBoolean())).thenReturn(true);
        assertThat(subject.cancel(false), is(true));
        verify(delegate).cancel(eq(false));
    }

    @Test
    public void testIsCancelled() {
        when(delegate.isCancelled()).thenReturn(true);
        assertThat(subject.isCancelled(), is(true));
        verify(delegate).isCancelled();
    }

    @Test
    public void testIsDone() {
        when(delegate.isDone()).thenReturn(true);
        assertThat(subject.isDone(), is(true));
        verify(delegate).isDone();
    }

    @Test
    public void testGet() throws ExecutionException, InterruptedException {
        Object result = new Object();
        when(delegate.get()).thenReturn(result);
        assertThat(subject.get(), is(sameInstance(result)));
        verify(delegate).get();
    }

    @Test
    public void testGetException() throws ExecutionException, InterruptedException {
        ExecutionException exception = new ExecutionException(new RuntimeException());
        when(delegate.get()).thenThrow(exception);
        try {
            subject.get();
            fail("Exception expected.");
        } catch (ExecutionException expected) {
            assertThat(expected, is(sameInstance(exception)));
        }
        verify(delegate).get();
    }

    @Test
    public void testGetTimeout() throws ExecutionException, InterruptedException, TimeoutException {
        Object result = new Object();
        when(delegate.get(anyLong(), any())).thenReturn(result);
        assertThat(subject.get(2387L, MILLISECONDS), is(sameInstance(result)));
        verify(delegate).get(eq(2387L), eq(MILLISECONDS));
    }

    @Test
    public void testGetTimeoutException() throws ExecutionException, InterruptedException, TimeoutException {
        ExecutionException exception = new ExecutionException(new RuntimeException());
        when(delegate.get(anyLong(), any())).thenThrow(exception);
        try {
            subject.get(1234L, SECONDS);
            fail("Exception expected.");
        } catch (ExecutionException expected) {
            assertThat(expected, is(sameInstance(exception)));
        }
        verify(delegate).get(eq(1234L), eq(SECONDS));
    }

    @Test
    public void testHashCode() {
        int hash = delegate.hashCode();
        assertThat(subject.hashCode(), is(hash));
    }

    @Test
    public void testEquals() {
        assertThat(subject, is(equalTo(subject)));
        assertThat(subject, is(equalTo((Object) new TestDelegatingFuture(delegate))));
        assertThat(subject, is(not(equalTo((Object) new TestDelegatingFuture(mock(Future.class))))));
    }

    @Test
    public void testToString() {
        assertThat(subject, hasToString("TestDelegatingFuture{" + delegate + "}"));
    }
}
