/*
 * Copyright 2016-2019 Talsma ICT
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
import nl.talsmasoftware.context.observer.ContextObservers;

import javax.servlet.ServletRequest;

/**
 * Simple (unstacked) context using a Threadlocal containing the current {@link ServletRequest}.
 *
 * @author Sjoerd Talsma
 */
final class ServletRequestContext implements Context<ServletRequest> {

    private static final ThreadLocal<ServletRequestContext> CONTEXT = new ThreadLocal<ServletRequestContext>();

    volatile ServletRequest servletRequest;

    ServletRequestContext(ServletRequest servletRequest) {
        this.servletRequest = servletRequest;
        CONTEXT.set(this);
        ContextObservers.onActivate(ServletRequestContextManager.class, servletRequest, null);
    }

    static Context<ServletRequest> current() {
        return CONTEXT.get();
    }

    public ServletRequest getValue() {
        return servletRequest;
    }

    public void close() {
        final ServletRequest deactivated = servletRequest;
        servletRequest = null;
        CONTEXT.set(null);
        ContextObservers.onDeactivate(ServletRequestContextManager.class, deactivated, null);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + (servletRequest == null ? "{closed}" : "{value=" + servletRequest + "}");
    }

}
