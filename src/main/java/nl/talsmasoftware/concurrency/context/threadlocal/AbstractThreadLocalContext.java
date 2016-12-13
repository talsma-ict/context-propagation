/*
 * Copyright (C) 2016 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.talsmasoftware.concurrency.context.threadlocal;

import nl.talsmasoftware.concurrency.context.Context;

/**
 * Abstract base class that will maintain a shared, static {@link ThreadLocal} instance for each concrete
 * subclass of this type. This threadlocal can be accessed by subclasses through the protected method:
 * {@link #threadLocalInstanceOf(Class)}.
 *
 * @author Sjoerd Talsma
 * @see nl.talsmasoftware.context.threadlocal.AbstractThreadLocalContext
 * @deprecated This is the old implementation.
 * Please swith to <code>nl.talsmasoftware.context.threadlocal.AbstractThreadLocalContext</code>
 */
public abstract class AbstractThreadLocalContext<T>
        extends nl.talsmasoftware.context.threadlocal.AbstractThreadLocalContext<T>
        implements Context<T> {

    /**
     * Instantiates a new context with the specified value.
     * The new context will be made the active context for the current thread.
     *
     * @param newValue The new value to become active in this new context
     *                 (or <code>null</code> to register a new context with 'no value').
     */
    @SuppressWarnings("unchecked")
    protected AbstractThreadLocalContext(T newValue) {
        super(newValue);
    }

}
