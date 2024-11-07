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

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter from {@link Callable} to {@link Runnable} ignoring any results.
 *
 * <p>
 * Normally it is a bad idea to ignore results; therefore this adapter class is limited to package-protected visibility.
 * It is used by the {@link CallMappingExecutorService} to re-use the {@link Callable} mapping functionality for mapping
 * {@link Runnable} objects as well. In that case, the {@Callable} being converted was already originally a
 * {@link Runnable} object, and this converter merely 'converts it back'.<br>
 * <em>In this particular usecase</em> the ignored result is indeed irrelevant.
 *
 * @author Sjoerd Talsma
 */
final class CallableToRunnable extends Wrapper<Callable<?>> implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(CallableToRunnable.class.getName());

    CallableToRunnable(Callable<?> callable) {
        super(callable);
        if (callable == null) throw new IllegalArgumentException("Callable to convert into runnable was <null>.");
    }

    public void run() {
        try {
            Object result = delegate().call();
            LOGGER.log(Level.FINEST, "Call result ignored by RunnableAdapter: {0}", result);
        } catch (RuntimeException unchecked) {
            throw unchecked;
        } catch (Exception checked) {
            throw new IllegalStateException("Checked exception thrown from call: " + checked.getMessage(), checked);
        }
    }

}
