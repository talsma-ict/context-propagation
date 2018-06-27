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
package nl.talsmasoftware.context.executors;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.delegation.CallMappingExecutorService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An instance of an executor service that delegates to another executor service that makes a new
 * {@link ContextManagers#createContextSnapshot() context snapshot} whenever a task is scheduled
 * (both {@link Callable} and {@link Runnable} tasks are mapped).
 *
 * @author Sjoerd Talsma
 */
public class ContextAwareExecutorService extends CallMappingExecutorService {
    private final Logger logger = Logger.getLogger(getClass().getName());

    public ContextAwareExecutorService(ExecutorService delegate) {
        super(delegate);
    }

    /**
     * This method maps any callable (before scheduling it) by taking a snapshot of the context in the scheduling thread
     * and propagating this context into the executed callable by snapshot reactivation.
     *
     * @param callable The callable to be mapped.
     * @param <V>      the actual return type of the callable object being scheduled.
     * @return A callable that will reactivate the scheduling thread context snapshot before executing.
     */
    @Override
    protected <V> Callable<V> map(final Callable<V> callable) {
        final ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        return new Callable<V>() {
            public V call() throws Exception {
                final Context<Void> context = snapshot.reactivate();
                boolean done = false;
                try {

                    final V result = callable.call();
                    done = true;
                    return result;

                } finally {
                    tryClose(context, done);
                }
            }
        };
    }

    /**
     * tryClose method to be called from a finally() block that properly manages close exceptions.
     *
     * @param context     The context to be closed.
     * @param callWasDone Whether or not the call was completed yet.
     */
    private void tryClose(Context<?> context, boolean callWasDone) {
        if (context != null) try {
            context.close();
        } catch (RuntimeException closeEx) {
            if (callWasDone) { // Call was already done; we have to re-throw the close error.
                throw closeEx;
            }
            // Call was not yet done, i.e. an exception was thrown.
            IllegalStateException exception = new IllegalStateException(
                    "Exception restoring context after applied snapshot: " + closeEx.getMessage(), closeEx);
            // Logging a warning is the best we can do; there is no SuppressedException in java 5.
            logger.log(Level.WARNING, exception.getMessage(), exception);
        }
    }
}
