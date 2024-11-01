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
package nl.talsmasoftware.context.functions;

import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A wrapper for {@link Runnable} that {@link ContextSnapshot#reactivate() reactivates a context snapshot} before
 * calling a delegate.
 *
 * @author Sjoerd Talsma
 * @deprecated Moved to package {@code nl.talsmasoftware.context.core.function}.
 */
@Deprecated
public class RunnableWithContext extends nl.talsmasoftware.context.core.function.RunnableWithContext {

    /**
     * Creates a new runnable that performs the following steps, in-order:
     * <ol>
     * <li>first {@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>then run the delegate
     * </ol>
     *
     * @param snapshot A snapshot for the contexts to run the delegate in.
     * @param delegate The delegate to run.
     * @see #RunnableWithContext(ContextSnapshot, Runnable, Consumer)
     */
    public RunnableWithContext(ContextSnapshot snapshot, Runnable delegate) {
        super(snapshot, delegate);
    }

    /**
     * Creates a new runnable that performs the following steps, in-order:
     * <ol>
     * <li>first {@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>then run the delegate
     * <li>finally, <em>if a consumer was provided</em>
     * {@linkplain ContextManagers#createContextSnapshot() capture a new ContextSnapshot}
     * </ol>
     *
     * @param snapshot A snapshot for the contexts to run the delegate in.
     * @param delegate The delegate to run.
     * @param consumer An optional consumer for the resulting contexts after the delegate ran (in case it changed)
     */
    public RunnableWithContext(ContextSnapshot snapshot, Runnable delegate, Consumer<ContextSnapshot> consumer) {
        super(snapshot, delegate, consumer);
    }

    protected RunnableWithContext(Supplier<ContextSnapshot> supplier, Runnable delegate, Consumer<ContextSnapshot> consumer) {
        super(supplier, delegate, consumer);
    }

}
