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
import org.junit.jupiter.api.function.Executable;

import java.util.concurrent.Callable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Pretty silly test that verifies that run method actually delegates to delegate.call().
 *
 * @author Sjoerd Talsma
 */
public class RunnableAdapterTest {

    Callable<Object> delegate;
    RunnableAdapter subject;

    @BeforeEach
    public void setUp() {
        delegate = mock(Callable.class);
        subject = new RunnableAdapter(delegate);
    }

    @AfterEach
    public void noMoreInteractions() {
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testNullConstructor() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            public void execute() {
                new RunnableAdapter(null);
            }
        });
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
