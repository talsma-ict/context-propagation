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

package nl.talsmasoftware.concurrency.context;

/**
 * Utility class to allow concurrent systems to {@link #createContextSnapshot() take snapshots of all contexts} from
 * known {@link ContextManager ContextManager} implementations.
 * <p>
 * Such a {@link ContextSnapshot snapshot} can be passed to a background task to allow the context to be
 * {@link ContextSnapshot#reactivate() reactivated} in that background thread, until it gets
 * {@link Context#close() closed} again (preferably in a <code>try-with-resources</code> construct).
 *
 * @author Sjoerd Talsma
 * @deprecated This is the old implementation. Please swith to <code>nl.talsmasoftware.context.ContextManagers</code>
 */
public final class ContextManagers {

    /**
     * Private constructor to avoid instantiation of this class.
     */
    private ContextManagers() {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is able to create a 'snapshot' from the current
     * {@link ContextManager#getActiveContext() active context} from <em>all known {@link ContextManager}</em>
     * implementations.
     * <p>
     * This snapshot is returned as a single object that can be temporarily
     * {@link ContextSnapshot#reactivate() reactivated}. Don't forget to {@link Context#close() close} the reactivated
     * context once you're done, preferably in a <code>try-with-resources</code> construct.
     *
     * @return A new snapshot that can be reactivated in a background thread within a try-with-resources construct.
     * @see nl.talsmasoftware.context.ContextManagers#createContextSnapshot()
     * @see LegacyContextManagerBridge
     * @deprecated This is the old implementation. Please swith to <code>nl.talsmasoftware.context.ContextManagers</code>
     */
    public static ContextSnapshot createContextSnapshot() {
        // Delegate to the new context manager implementation.
        // Depend on our LegacyContextManager to propagate old contexts.
        final nl.talsmasoftware.context.ContextSnapshot newSnapshot =
                nl.talsmasoftware.context.ContextManagers.createContextSnapshot();
        return new ContextSnapshot() {
            @Override
            public Context<Void> reactivate() {
                final nl.talsmasoftware.context.Context<Void> newReactivation = newSnapshot.reactivate();
                return new Context<Void>() {
                    @Override
                    public Void getValue() {
                        return null;
                    }

                    @Override
                    public void close() {
                        newReactivation.close();
                    }
                };
            }
        };
    }

}
