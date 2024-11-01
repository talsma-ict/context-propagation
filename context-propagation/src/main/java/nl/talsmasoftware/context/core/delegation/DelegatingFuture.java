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
package nl.talsmasoftware.context.core.delegation;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Abstract baseclass that simplifies wrapping an existing {@link Future} by forwarding all required methods to a
 * {@link nl.talsmasoftware.context.delegation.Wrapper#delegate() delegate future} object.<br>
 * The class also provides overridable wrapper methods for {@link #wrapResult(Object) result}
 * and {@link #wrapException(ExecutionException) exception} outcomes.
 * <p>
 * Although this class does implement <em>all</em> required methods of {@link Future} it is still declared as an
 * <em>abstract</em> class.<br>
 * This is because it does not provide any value in itself.
 *
 * @author Sjoerd Talsma
 */
public abstract class DelegatingFuture<V> extends nl.talsmasoftware.context.delegation.Wrapper<Future<V>> implements Future<V> {

    protected DelegatingFuture(Future<V> delegate) {
        super(delegate);
    }

    /**
     * Overridable method to wrap the result after it has been obtained from the delegate future.
     *
     * @param result The original result from the delegate.
     * @return The wrapped result.
     */
    protected V wrapResult(V result) {
        return result;
    }

    /**
     * Overridable method to wrap the {@link ExecutionException} after it has been thrown from the delegate future.
     *
     * @param exception The original execution exception from the delegate.
     * @return The wrapped exception.
     */
    protected ExecutionException wrapException(ExecutionException exception) {
        return exception;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate().cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return delegate().isCancelled();
    }

    public boolean isDone() {
        return delegate().isDone();
    }

    public V get() throws InterruptedException, ExecutionException {
        try {
            return wrapResult(delegate().get());
        } catch (ExecutionException ee) {
            throw wrapException(ee);
        }
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return wrapResult(delegate().get(timeout, unit));
        } catch (ExecutionException ee) {
            throw wrapException(ee);
        }
    }
}
