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

package nl.talsmasoftware.concurrency.futures;

import java.util.concurrent.Future;

import static java.util.Objects.requireNonNull;

/**
 * Abstract baseclass that simplifies wrapping an existing {@link Future} by forwarding all required methods to a
 * delegate future object.
 * <p>
 * Although this class implements all required methods of {@link Future} it is still declared as an
 * <em>abstract</em> class. This is because it does not provide any value in itself.
 *
 * @author Sjoerd Talsma
 * @see nl.talsmasoftware.context.delegation.DelegatingFuture
 * @deprecated Please switch to <code>nl.talsmasoftware.context.delegation.DelegatingFuture</code>
 */
public abstract class DelegatingFuture<V> extends nl.talsmasoftware.context.delegation.DelegatingFuture<V> {

    /**
     * @see #nonNullDelegate()
     * @deprecated Please use {@link #nonNullDelegate()}.
     */
    protected final Future<V> delegate;

    protected DelegatingFuture(Future<V> delegate) {
        super(requireNonNull(delegate, "No delegate future provided!"));
        this.delegate = nonNullDelegate();
    }

}
