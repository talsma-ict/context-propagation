/*
 * Copyright 2016-2026 Talsma ICT
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
import nl.talsmasoftware.context.core.delegation.DelegatingExecutorService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Executor service that wraps another {@linkplain ExecutorService}, making sure background tasks operate 'within'
 * a reactivated {@linkplain ContextSnapshot context snapshot} that was captured from the submitting thread.
 *
 * <p>
 * Any existing {@linkplain ExecutorService} can be used as a delegate, including those from
 * the {@linkplain java.util.concurrent.Executors Executors} utility class.
 *
 * <p>
 * The executor service makes sure to close each reactivated snapshot again after the code in the task finishes,
 * even if an exception was thrown.
 *
 * <p>
 * Both {@link Callable} and {@link Runnable} tasks are wrapped.
 * {@linkplain ContextSnapshot} contains a static {@linkplain ContextSnapshot#wrap(Runnable) wrap}
 * method for {@linkplain Runnable} and {@linkplain Callable}.
 *
 * @author Sjoerd Talsma
 */
public final class ContextAwareExecutorService extends DelegatingExecutorService implements ExecutorService {
    /**
     * Wrap an {@linkplain ExecutorService}, making it <em>context-aware</em>.
     *
     * <p>
     * The new executor service passes all tasks to the {@code delegate} executor service,
     * capturing a {@linkplain ContextSnapshot} from the caller thread.<br>
     * Submitted tasks will reactivate (and close) this snapshot in the executed thread context.
     *
     * @param delegate The delegate executor service to submit tasks to.
     * @return The new context-aware executor service.
     * @see ContextSnapshot#capture()
     * @see ContextSnapshot#reactivate()
     */
    public static ContextAwareExecutorService wrap(ExecutorService delegate) {
        return new ContextAwareExecutorService(delegate);
    }

    private ContextAwareExecutorService(ExecutorService delegate) {
        super(delegate);
    }

    @Override
    protected <T> Callable<T> wrap(final Callable<T> callable) {
        return ContextSnapshot.capture().wrap(callable);
    }

    @Override
    protected Runnable wrap(final Runnable runnable) {
        return ContextSnapshot.capture().wrap(runnable);
    }
}
