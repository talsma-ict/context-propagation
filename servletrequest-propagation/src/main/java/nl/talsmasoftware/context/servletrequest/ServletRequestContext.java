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

import nl.talsmasoftware.context.core.threadlocal.AbstractThreadLocalContext;

import javax.servlet.ServletRequest;

/**
 * Simple (unstacked) context using a ThreadLocal containing the current {@link ServletRequest}.
 *
 * @author Sjoerd Talsma
 */
final class ServletRequestContext extends AbstractThreadLocalContext<ServletRequest> {
    private static final ThreadLocal<ServletRequestContext> CONTEXT =
            AbstractThreadLocalContext.threadLocalInstanceOf(ServletRequestContext.class);

    ServletRequestContext(ServletRequest servletRequest) {
        super(servletRequest);
    }

    static ServletRequest currentValue() {
        ServletRequestContext current = CONTEXT.get();
        return current != null ? current.getValue() : null;
    }

    static void clear() {
        try {
            for (ServletRequestContext current = CONTEXT.get(); current != null; current = CONTEXT.get()) {
                current.close();
            }
        } finally {
            CONTEXT.remove();
        }
    }
}
