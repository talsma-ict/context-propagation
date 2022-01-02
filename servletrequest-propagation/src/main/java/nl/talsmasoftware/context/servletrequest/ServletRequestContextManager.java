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

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManager;

import javax.servlet.ServletRequest;

/**
 * Manager to propagate a current {@link ServletRequest} with.
 *
 * @author Sjoerd Talsma
 */
public final class ServletRequestContextManager implements ContextManager<ServletRequest> {

    /**
     * Static utility method to obtain the current {@link ServletRequest} (if available).
     *
     * @return The current ServletRequest if available, or <code>null</code> otherwise.
     */
    public static ServletRequest currentServletRequest() {
        Context<ServletRequest> current = ServletRequestContext.current();
        return current == null ? null : current.getValue();
    }

    public Context<ServletRequest> initializeNewContext(ServletRequest value) {
        return new ServletRequestContext(value);
    }

    public Context<ServletRequest> getActiveContext() {
        return ServletRequestContext.current();
    }

    /**
     * Unconditionally removes the active context (and any parents).
     * <p>
     * This is useful for boundary filters, whose Threads may be returned to some threadpool.
     */
    public static void clear() {
        Context<ServletRequest> current = ServletRequestContext.current();
        if (current != null) {
            current.close();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
