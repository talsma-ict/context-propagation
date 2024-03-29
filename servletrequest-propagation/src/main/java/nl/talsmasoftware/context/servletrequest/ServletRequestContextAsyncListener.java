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
package nl.talsmasoftware.context.servletrequest;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

/**
 * An async listener that creates servlet request context when requests
 * are started and clears the context on completion.
 *
 * @author Sjoerd Talsma
 */
final class ServletRequestContextAsyncListener implements AsyncListener {

    public void onStartAsync(AsyncEvent event) {
        event.getAsyncContext().addListener(this);
        new ServletRequestContext(event.getSuppliedRequest()); // Become the new active context
    }

    public void onComplete(AsyncEvent event) {
        ServletRequestContextManager.clear();
    }

    public void onTimeout(AsyncEvent event) {
        ServletRequestContextManager.clear();
    }

    public void onError(AsyncEvent event) {
        ServletRequestContextManager.clear();
    }
}
