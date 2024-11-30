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
package nl.talsmasoftware.context.core.function;

import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.core.ContextManagers;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * {@linkplain Runnable} that {@linkplain ContextSnapshot#reactivate() reactivates a context snapshot} during the task.
 *
 * <p>
 * Implemented as a {@linkplain nl.talsmasoftware.context.core.delegation.Wrapper Wrapper} for {@linkplain Runnable}.
 *
 * <p>
 * The reactivated context snapshot will be safely closed after the task has completed.
 *
 * @author Sjoerd Talsma
 */
public class RunnableWithContext extends WrapperWithContextAndConsumer<Runnable> implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(RunnableWithContext.class.getName());

    /**
     * Creates a new runnable that performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>run the delegate task
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * </ol>
     *
     * @param snapshot Context snapshot to run the delegate task in.
     * @param delegate The delegate task to run.
     * @see #RunnableWithContext(ContextSnapshot, Runnable, Consumer)
     */
    public RunnableWithContext(ContextSnapshot snapshot, Runnable delegate) {
        this(snapshot, delegate, null);
    }

    /**
     * Creates a new runnable that performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>run the delegate task
     * <li><em>if snapshot consumer is non-null,</em>
     * pass a {@linkplain ContextManagers#createContextSnapshot() new context snapshot} to the consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * </ol>
     *
     * @param snapshot         Context snapshot to run the delegate task in.
     * @param delegate         The delegate task to run.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate task ran
     *                         (optional, may be {@code null}).
     */
    public RunnableWithContext(ContextSnapshot snapshot, Runnable delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshot, delegate, snapshotConsumer);
    }

    /**
     * Protected constructor for use with a snapshot 'holder' object that acts as both snapshot supplier and -consumer.
     *
     * <p>
     * This constructor is not for general use. Care must be taken to capture the context snapshot <em>before</em> the
     * consumer is called, otherwise the snapshot being reactivated would effectively update the context
     * to the same values just captured.
     *
     * @param snapshotSupplier Supplier for the context snapshot that was previously captured.
     * @param delegate         The delegate task to run.
     * @param snapshotConsumer Consumer accepting the resulting context snapshot after the delegate task ran
     *                         (optional, may be {@code null}).
     */
    protected RunnableWithContext(Supplier<ContextSnapshot> snapshotSupplier, Runnable delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshotSupplier, delegate, snapshotConsumer);
    }

    /**
     * Run the delegate task in a reactivated context snapshot.
     *
     * <p>
     * The implementation performs the following steps, in-order:
     * <ol>
     * <li>{@linkplain ContextSnapshot#reactivate() reactivate} the given snapshot
     * <li>run the delegate task
     * <li><em>if context snapshot consumer is non-null,</em>
     * pass a {@linkplain ContextManagers#createContextSnapshot() new context snapshot} to the consumer
     * <li>close the {@linkplain ContextSnapshot.Reactivation reactivation}
     * </ol>
     */
    @Override
    public void run() {
        try (ContextSnapshot.Reactivation context = snapshot().reactivate()) {
            try { // inner 'try' is needed: https://github.com/talsma-ict/context-propagation/pull/56#discussion_r201590623
                LOGGER.finest(() -> "Delegating run method with " + context + " to " + delegate() + ".");
                delegate().run();
            } finally {
                if (contextSnapshotConsumer != null) {
                    ContextSnapshot resultSnapshot = ContextSnapshot.capture();
                    LOGGER.finest(() -> "Captured context snapshot after delegation: " + resultSnapshot + ".");
                    contextSnapshotConsumer.accept(resultSnapshot);
                }
            }
        }
    }

}
