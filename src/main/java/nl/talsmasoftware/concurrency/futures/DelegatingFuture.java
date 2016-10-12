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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

/**
 * Abstract baseclass that simplifies wrapping an existing {@link Future} by forwarding all required methods to a
 * delegate future object.
 * <p>
 * Although this class implements all required methods of {@link Future} it is still declared as an
 * <em>abstract</em> class. This is because it does not provide any value in itself.
 *
 * @author Sjoerd Talsma
 */
public abstract class DelegatingFuture<V> implements Future<V> {

    protected final Future<V> delegate;

    protected DelegatingFuture(Future<V> delegate) {
        this.delegate = requireNonNull(delegate, "No delegate future provided!");
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    public boolean isDone() {
        return delegate.isDone();
    }

    public V get() throws InterruptedException, ExecutionException {
        return delegate.get();
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.get(timeout, unit);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other != null && getClass().equals(other.getClass())
                && delegate.equals(((DelegatingFuture) other).delegate));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + delegate + '}';
    }

}
