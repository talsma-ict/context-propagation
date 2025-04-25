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

import nl.talsmasoftware.context.api.ContextManager;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletRequest;

/**
 * An {@linkplain AsyncListener async listener} that creates servlet request contexts when requests
 * are started and clears them on completion.
 *
 * @author Sjoerd Talsma
 */
final class ServletRequestContextAsyncListener implements AsyncListener {
    private static final ContextManager<ServletRequest> MANAGER = ServletRequestContextManager.provider();

    /**
     * Registers itself for future updates and initializes the supplied servlet request as the current servlet request
     * on the async thread.
     *
     * @param event the AsyncEvent indicating that a new asynchronous cycle is being initiated.
     * @see ServletRequestContextManager#initializeNewContext(ServletRequest)
     */
    public void onStartAsync(AsyncEvent event) {
        event.getAsyncContext().addListener(this);
        MANAGER.initializeNewContext(event.getSuppliedRequest());
    }

    /**
     * {@linkplain ServletRequestContextManager#clear() Clears} the servlet request context again
     * after the request ends by completion.
     *
     * @param event the AsyncEvent indicating that an asynchronous operation has been completed
     */
    public void onComplete(AsyncEvent event) {
        MANAGER.clear();
    }

    /**
     * {@linkplain ServletRequestContextManager#clear() Clears} the servlet request context again
     * after the request ends by timeout.
     *
     * @param event the AsyncEvent indicating that an asynchronous operation has timed out
     */
    public void onTimeout(AsyncEvent event) {
        MANAGER.clear();
    }

    /**
     * {@linkplain ServletRequestContextManager#clear() Clears} the servlet request context again
     * after the request ends by an error.
     *
     * @param event the AsyncEvent indicating that an asynchronous operation has failed to complete
     */
    public void onError(AsyncEvent event) {
        MANAGER.clear();
    }
}
