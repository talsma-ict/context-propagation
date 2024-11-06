/*
 * Copyright 2016-2024 Talsma ICT
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
package nl.talsmasoftware.context.servletrequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ServletRequestContextAsyncListenerTest {

    ServletRequestContextAsyncListener subject;

    AsyncEvent event;
    AsyncContext mockContext;
    ServletRequest mockRequest;
    ServletResponse mockResponse;

    @BeforeEach
    public void setUp() {
        ServletRequestContext.clear();
        mockContext = mock(AsyncContext.class);
        mockRequest = mock(ServletRequest.class);
        mockResponse = mock(ServletResponse.class);
        event = new AsyncEvent(mockContext, mockRequest, mockResponse);

        subject = new ServletRequestContextAsyncListener();
    }

    @AfterEach
    public void tearDown() {
        ServletRequestContext.clear();
        verifyNoMoreInteractions(mockContext, mockRequest, mockResponse);
    }

    @Test
    public void testOnStart() {
        assertThat(ServletRequestContextManager.currentServletRequest(), is(nullValue()));

        subject.onStartAsync(event);
        assertThat(ServletRequestContextManager.currentServletRequest(), is(sameInstance(mockRequest)));

        verify(mockContext).addListener(eq(subject));
    }

    @Test
    public void testOnComplete() {
        new ServletRequestContext(mockRequest);
        assertThat(ServletRequestContextManager.currentServletRequest(), is(sameInstance(mockRequest)));

        subject.onComplete(event);
        assertThat(ServletRequestContextManager.currentServletRequest(), is(nullValue()));
    }

    @Test
    public void testOnTimeout() {
        new ServletRequestContext(mockRequest);
        assertThat(ServletRequestContextManager.currentServletRequest(), is(sameInstance(mockRequest)));

        subject.onTimeout(event);
        assertThat(ServletRequestContextManager.currentServletRequest(), is(nullValue()));
    }

    @Test
    public void testOnError() {
        new ServletRequestContext(mockRequest);
        assertThat(ServletRequestContextManager.currentServletRequest(), is(sameInstance(mockRequest)));

        subject.onError(event);
        assertThat(ServletRequestContextManager.currentServletRequest(), is(nullValue()));
    }

}
