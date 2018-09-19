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
package nl.talsmasoftware.concurrency.context;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * @author Sjoerd Talsma
 * @deprecated This class serves as a 'bridge' to propagate deprecated ContextManagers through the new implementation.
 */
public class LegacyContextManagerBridge implements nl.talsmasoftware.context.ContextManager<ContextSnapshot> {

    //
    // Bridging functionality for propagating old snapshots through the new library:
    //

    @Override
    @SuppressWarnings("unchecked")
    public nl.talsmasoftware.context.Context<ContextSnapshot> initializeNewContext(ContextSnapshot value) {
        final nl.talsmasoftware.context.Context<?> reactivation = value != null ? value.reactivate() : null;
        return (nl.talsmasoftware.context.Context<ContextSnapshot>) reactivation;
    }

    @Override
    public nl.talsmasoftware.context.Context<ContextSnapshot> getActiveContext() {
        final ContextSnapshot legacySnapshot = createLegacyContextSnapshot();
        return new nl.talsmasoftware.context.Context<ContextSnapshot>() {
            @Override
            public ContextSnapshot getValue() {
                return legacySnapshot;
            }

            @Override
            public void close() {
                // Snapshot was already taken and has no meaning to be closed.
            }
        };
    }

    //
    // Legacy code propagating old Contexts:
    //

    private static final Logger LOGGER = Logger.getLogger(ContextManagers.class.getName());

    private static ContextSnapshot createLegacyContextSnapshot() {
        final Map<ContextManager, Object> snapshot = new IdentityHashMap<ContextManager, Object>();
        boolean empty = true;
        for (ContextManager legacyManager : ServiceLoader.load(ContextManager.class, LegacyContextManagerBridge.class.getClassLoader())) {
            empty = false;
            final nl.talsmasoftware.concurrency.context.Context activeLegacyContext = legacyManager.getActiveContext();
            if (activeLegacyContext != null) snapshot.put(legacyManager, activeLegacyContext.getValue());
        }
        if (empty) {
            LOGGER.finer("No legacy context managers found. It's probably safe to remove the legacy bridge from your application.");
        }
        return snapshot.isEmpty() ? LegacyContextSnapshot.EMPTY : new LegacyContextSnapshot(snapshot);
    }

    /**
     * Implementation of the <code>createContextSnapshot</code> functionality that can reactivate all values from the
     * snapshot in each corresponding {@link ContextManager}.
     */
    private static final class LegacyContextSnapshot implements ContextSnapshot {
        private static ContextSnapshot EMPTY = new ContextSnapshot() {
            @Override
            public Context<Void> reactivate() {
                return ReactivatedContext.EMPTY;
            }

            @Override
            public String toString() {
                return "LegacyContextSnapshot{size=0}";
            }
        };

        private final Map<ContextManager, Object> snapshot;

        LegacyContextSnapshot(Map<ContextManager, Object> snapshot) {
            this.snapshot = snapshot;
        }

        @SuppressWarnings("unchecked") // We got the value from the context manager itself!
        public nl.talsmasoftware.concurrency.context.Context<Void> reactivate() {
            final List<nl.talsmasoftware.concurrency.context.Context<?>> reactivatedContexts = new ArrayList<nl.talsmasoftware.concurrency.context.Context<?>>(snapshot.size());
            try {
                for (Map.Entry<ContextManager, Object> entry : snapshot.entrySet()) {
                    reactivatedContexts.add(entry.getKey().initializeNewContext(entry.getValue()));
                }
                return new ReactivatedContext(reactivatedContexts);
            } catch (RuntimeException errorWhileReactivating) {
                for (nl.talsmasoftware.concurrency.context.Context reactivated : reactivatedContexts) {
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
            return "LegacyContextSnapshot{size=" + snapshot.size() + '}';
        }
    }

    /**
     * Implementation of the reactivated container context that closes all reactivated contexts when it is closed
     * itself. This context contains no value of itself.
     */
    private static final class ReactivatedContext implements Context<Void> {
        private static Context<Void> EMPTY = new Context<Void>() {
            @Override
            public Void getValue() {
                return null;
            }

            @Override
            public void close() {
                // no-op
            }

            @Override
            public String toString() {
                return "ReactivatedLegacyContext{size=0}";
            }
        };

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
            return "ReactivatedLegacyContext{size=" + reactivated.size() + '}';
        }
    }


}
