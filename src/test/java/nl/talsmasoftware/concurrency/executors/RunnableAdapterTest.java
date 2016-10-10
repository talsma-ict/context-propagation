/*
 * Copyright (C) 2016 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.talsmasoftware.concurrency.executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Pretty silly test that verifies that run method actually delegates to delegate.call().
 *
 * @author Sjoerd Talsma
 */
public class RunnableAdapterTest {

    Callable<Object> delegate;
    RunnableAdapter subject;

    @Before
    public void setUp() {
        delegate = mock(Callable.class);
        subject = new RunnableAdapter(delegate);
    }

    @After
    public void noMoreInteractions() {
        verifyNoMoreInteractions(delegate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullConstructor() {
        new RunnableAdapter(null);
    }

    @Test
    public void testRun() throws Exception {
        when(delegate.call()).thenReturn(new Object());
        subject.run();
        verify(delegate).call();
    }

    @Test
    public void testRunException() throws Exception {
        RuntimeException exception = new IllegalStateException("oops!");
        when(delegate.call()).thenThrow(exception);
        try {
            subject.run();
            fail("Exception expected.");
        } catch (RuntimeException expected) {
            assertThat(expected, is(sameInstance(exception)));
        }
        verify(delegate).call();
    }

    @Test
    public void testRunCheckedException() throws Exception {
        Exception exception = new Exception("oops!");
        when(delegate.call()).thenThrow(exception);
        try {
            subject.run();
            fail("Exception expected.");
        } catch (RuntimeException expected) {
            assertThat(expected.getCause(), is(sameInstance((Throwable) exception)));
        }
        verify(delegate).call();
    }

    @Test
    public void testHashCode() {
        int hash = delegate.hashCode();
        assertThat(subject.hashCode(), is(hash));
    }

    @Test
    public void testEquals() {
        assertThat(subject, is(equalTo(subject)));
        assertThat(subject, is(equalTo((Object) new RunnableAdapter(delegate))));
        assertThat(subject, is(not(equalTo((Object) new RunnableAdapter(mock(Callable.class))))));
    }

    @Test
    public void testToString() {
        assertThat(subject, hasToString("RunnableAdapter{" + delegate + "}"));
    }

}
