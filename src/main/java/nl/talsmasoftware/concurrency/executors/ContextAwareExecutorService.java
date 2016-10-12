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

package nl.talsmasoftware.concurrency.executors;

import nl.talsmasoftware.concurrency.context.Context;
import nl.talsmasoftware.concurrency.context.ContextManagers;
import nl.talsmasoftware.concurrency.context.ContextSnapshot;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An instance of an executor service that delegates to another executor service that makes a new
 * {@link ContextManagers#createContextSnapshot() context snapshot} whenever a task is scheduled (either a
 *
 * @author Sjoerd Talsma
 */
public class ContextAwareExecutorService extends CallMappingExecutorService {
    private final Logger logger = Logger.getLogger(getClass().getName());

    public ContextAwareExecutorService(ExecutorService delegate) {
        super(delegate);
    }

    @Override
    protected <V> Callable<V> map(final Callable<V> callable) {
        final ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        return () -> {
            try (Context<Void> context = snapshot.reactivate()) {
                logger.log(Level.FINEST, "Propagated {0} for call: {1}.", new Object[]{context, callable});
                return callable.call();
            }
        };
    }


}
