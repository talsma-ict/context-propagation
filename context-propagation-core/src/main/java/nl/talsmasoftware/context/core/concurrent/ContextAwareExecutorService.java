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
package nl.talsmasoftware.context.core.concurrent;

import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.api.ContextSnapshot.Reactivation;
import nl.talsmasoftware.context.core.ContextManagers;
import nl.talsmasoftware.context.core.delegation.CallMappingExecutorService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Executor service that wraps another executor service, making sure background tasks operates 'within'
 * a context snapshot taken from the submitting thread.
 *
 * <p>
 * The executor service will make sure to close the reactivated snapshot again after the code in the task is finished,
 * even if it throws an exception.
 *
 * <p>
 * Both {@link Callable} and {@link Runnable} tasks are mapped.
 *
 * @author Sjoerd Talsma
 */
public class ContextAwareExecutorService extends CallMappingExecutorService {
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
        return () -> {
            try (Reactivation reactivation = snapshot.reactivate()) {
                return callable.call();
            }
        };
    }
}
