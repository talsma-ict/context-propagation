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
package nl.talsmasoftware.context.functions;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.delegation.WrapperWithContext;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper for {@link Runnable} that {@link ContextSnapshot#reactivate() reactivates a context snapshot} before
 * calling a delegate.
 *
 * @author Sjoerd Talsma
 */
public class RunnableWithContext extends WrapperWithContext<Runnable> implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(RunnableWithContext.class.getName());

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
        this(snapshot, delegate, null);
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
        super(snapshot, delegate, consumer == null ? null : consumer::accept);
    }

    @Override
    public void run() {
        try (Context<Void> context = snapshot.reactivate()) {
            try {
                LOGGER.log(Level.FINEST, "Delegating run method with {0} to {1}.", new Object[]{context, delegate()});
                nonNullDelegate().run();
            } finally {
                if (consumer != null) {
                    ContextSnapshot resultSnapshot = ContextManagers.createContextSnapshot();
                    LOGGER.log(Level.FINEST, "Captured context snapshot after delegation: {0}", resultSnapshot);
                    consumer.accept(resultSnapshot);
                }
            }
        }
    }

}
