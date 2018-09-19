/*
 * Copyright 2016-2018 Talsma ICT
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
package nl.talsmasoftware.concurrency.executors;

import java.util.concurrent.ExecutorService;

/**
 * Abstract baseclass that makes it a little easier to wrap existing {@link ExecutorService} implementations by
 * forwarding all methods to a <code>delegate</code> executor service.
 * <p>
 * Although this class implements all required methods of {@link ExecutorService} it is still declared as an
 * <em>abstract</em> class. This is because it does not provide any value in itself.
 *
 * @author Sjoerd Talsma
 * @see nl.talsmasoftware.context.delegation.DelegatingExecutorService
 * @deprecated Please switch to <code>nl.talsmasoftware.context.delegation.DelegatingExecutorService</code>
 */
public abstract class DelegatingExecutorService extends nl.talsmasoftware.context.delegation.DelegatingExecutorService {

    /**
     * Subclass-accessible reference to the delegate executor service being wrapped.
     *
     * @deprecated Please call {@link #nonNullDelegate()} instead.
     */
    protected final ExecutorService delegate;

    protected DelegatingExecutorService(ExecutorService delegate) {
        super(delegate);
        this.delegate = super.nonNullDelegate();
    }

}
