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
package nl.talsmasoftware.context.managers.servletrequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.AsyncContext;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ServletRequestContextFilterTest {

    ServletRequestContextFilter subject;

    ServletRequest mockRequest;
    ServletResponse mockResponse;
    FilterConfig mockConfig;

    @BeforeEach
    void setUp() {
        mockRequest = mock(ServletRequest.class);
        mockResponse = mock(ServletResponse.class);
        mockConfig = mock(FilterConfig.class);

        subject = new ServletRequestContextFilter();
        subject.init(mockConfig);
    }

    @AfterEach
    void tearDown() {
        subject.destroy();
        verifyNoMoreInteractions(mockRequest, mockResponse, mockConfig);
    }

    @Test
    void testToString() {
        assertThat(subject).hasToString(ServletRequestContextFilter.class.getSimpleName());
    }

    @Test
    void testSimpleFiltering() throws IOException, ServletException {
        when(mockRequest.isAsyncStarted()).thenReturn(false);

        subject.doFilter(mockRequest, mockResponse,
                (request, response) ->
                        assertThat(ServletRequestContextManager.currentServletRequest()).isSameAs(request));

        verify(mockRequest).isAsyncStarted();
    }

    @Test
    void testAsyncFiltering() throws IOException, ServletException {
        AsyncContext mockAsyncContext = mock(AsyncContext.class);
        when(mockRequest.isAsyncStarted()).thenReturn(true);
        when(mockRequest.getAsyncContext()).thenReturn(mockAsyncContext);

        subject.doFilter(mockRequest, mockResponse,
                (request, response) ->
                        assertThat(ServletRequestContextManager.currentServletRequest()).isSameAs(request));

        verify(mockRequest).isAsyncStarted();
        verify(mockRequest).getAsyncContext();
        verify(mockAsyncContext).addListener(any(ServletRequestContextAsyncListener.class));
        verifyNoMoreInteractions(mockAsyncContext);
    }

    @Test
    void testAsyncFiltering_exceptionInAddListener() throws IOException, ServletException {
        AsyncContext mockAsyncContext = mock(AsyncContext.class);
        when(mockRequest.isAsyncStarted()).thenReturn(true);
        when(mockRequest.getAsyncContext()).thenReturn(mockAsyncContext);
        doThrow(IllegalStateException.class).when(mockAsyncContext).addListener(any(ServletRequestContextAsyncListener.class));

        subject.doFilter(mockRequest, mockResponse,
                (request, response) ->
                        assertThat(ServletRequestContextManager.currentServletRequest()).isSameAs(request));

        verify(mockRequest).isAsyncStarted();
        verify(mockRequest).getAsyncContext();
        verify(mockAsyncContext).addListener(any(ServletRequestContextAsyncListener.class));
        verifyNoMoreInteractions(mockAsyncContext);
    }
}
