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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to allow concurrent systems to {@link #createContextSnapshot() take snapshots of all contexts} from
 * known {@link ContextManager ContextManager} implementations.
 * <p>
 * Such a {@link ContextSnapshot snapshot} can be passed to a background task to allow the context to be
 * {@link ContextSnapshot#reactivate() reactivated} in that background thread, until it gets
 * {@link Context#close() closed} again (preferably in a <code>try-with-resources</code> construct).
 *
 * @author Sjoerd Talsma
 */
public final class ContextManagers {

    private static final Logger LOGGER = Logger.getLogger(ContextManagers.class.getName());

    /**
     * Service locator for registered {@link ContextManager} implementations.
     */
    private static final ServiceLoader<ContextManager> LOCATOR =
            ServiceLoader.load(ContextManager.class, ContextManagers.class.getClassLoader());

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
     */
    public static ContextSnapshot createContextSnapshot() {
        final Map<ContextManager, Object> snapshot = new IdentityHashMap<ContextManager, Object>();
        boolean empty = true;
        for (ContextManager manager : LOCATOR) {
            empty = false;
            final Context activeContext = manager.getActiveContext();
            if (activeContext != null) snapshot.put(manager, activeContext.getValue());
        }
        if (empty) LOGGER.log(Level.WARNING, "Context snapshot was created but no ContextManagers were found!");
        return new ContextSnapshotImpl(snapshot);
    }

    /**
     * Implementation of the <code>createContextSnapshot</code> functionality that can reactivate all values from the
     * snapshot in each corresponding {@link ContextManager}.
     */
    private static final class ContextSnapshotImpl implements ContextSnapshot {
        private final Map<ContextManager, Object> snapshot;

        ContextSnapshotImpl(Map<ContextManager, Object> snapshot) {
            this.snapshot = snapshot;
        }

        @SuppressWarnings("unchecked") // We got the value from the context manager itself!
        public Context<Void> reactivate() {
            final List<Context<?>> reactivatedContexts = new ArrayList<Context<?>>(snapshot.size());
            try {
                for (Map.Entry<ContextManager, Object> entry : snapshot.entrySet()) {
                    reactivatedContexts.add(entry.getKey().initializeNewContext(entry.getValue()));
                }
                return new ReactivatedContext(reactivatedContexts);
            } catch (RuntimeException errorWhileReactivating) {
                for (Context reactivated : reactivatedContexts) {
                    if (reactivated != null) try {
                        reactivated.close();
                    } catch (RuntimeException rte) {
                        errorWhileReactivating.addSuppressed(rte);
                    }
                }
                throw errorWhileReactivating;
            }
        }

        @Override
        public String toString() {
            return "ContextSnapshot{size=" + snapshot.size() + '}';
        }
    }

    /**
     * Implementation of the reactivated container context that closes all reactivated contexts when it is closed
     * itself. This context contains no value of itself.
     */
    private static final class ReactivatedContext implements Context<Void> {
        private final List<Context<?>> reactivated;

        private ReactivatedContext(List<Context<?>> reactivated) {
            this.reactivated = reactivated;
        }

        public Void getValue() {
            return null;
        }

        public void close() {
            RuntimeException closeError = null;
            for (Context<?> reactivated : this.reactivated) {
                if (reactivated != null) try {
                    reactivated.close();
                } catch (RuntimeException rte) {
                    if (closeError == null) closeError = rte;
                    else closeError.addSuppressed(rte);
                }
            }
            if (closeError != null) throw closeError;
        }

        @Override
        public String toString() {
            return "ReactivatedContext{size=" + reactivated.size() + '}';
        }
    }

}
