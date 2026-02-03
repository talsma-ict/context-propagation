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
package nl.talsmasoftware.context.managers.servletrequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ServletRequestContextAsyncListenerTest {

    ServletRequestContextAsyncListener subject;

    AsyncEvent event;
    AsyncContext mockContext;
    ServletRequest mockRequest;
    ServletResponse mockResponse;

    @BeforeEach
    void setUp() {
        ServletRequestContext.clear();
        mockContext = mock(AsyncContext.class);
        mockRequest = mock(ServletRequest.class);
        mockResponse = mock(ServletResponse.class);
        event = new AsyncEvent(mockContext, mockRequest, mockResponse);

        subject = new ServletRequestContextAsyncListener(ServletRequestContextManager.provider());
    }

    @AfterEach
    void tearDown() {
        ServletRequestContext.clear();
        verifyNoMoreInteractions(mockContext, mockRequest, mockResponse);
    }

    @Test
    void testConstructorWithNullManager() {
        assertThatThrownBy(() -> new ServletRequestContextAsyncListener(null))
                .isInstanceOf(NullPointerException.class).message()
                .containsIgnoringCase("context manager")
                .containsIgnoringCase("is <null>");
    }

    @Test
    void testOnStart() {
        assertThat(ServletRequestContextManager.currentServletRequest()).isNull();

        subject.onStartAsync(event);
        assertThat(ServletRequestContextManager.currentServletRequest()).isSameAs(mockRequest);

        verify(mockContext).addListener(subject);
    }

    @Test
    void testOnComplete() {
        new ServletRequestContext(mockRequest);
        assertThat(ServletRequestContextManager.currentServletRequest()).isSameAs(mockRequest);

        subject.onComplete(event);
        assertThat(ServletRequestContextManager.currentServletRequest()).isNull();
    }

    @Test
    void testOnTimeout() {
        new ServletRequestContext(mockRequest);
        assertThat(ServletRequestContextManager.currentServletRequest()).isSameAs(mockRequest);

        subject.onTimeout(event);
        assertThat(ServletRequestContextManager.currentServletRequest()).isNull();
    }

    @Test
    void testOnError() {
        new ServletRequestContext(mockRequest);
        assertThat(ServletRequestContextManager.currentServletRequest()).isSameAs(mockRequest);

        subject.onError(event);
        assertThat(ServletRequestContextManager.currentServletRequest()).isNull();
    }

}
