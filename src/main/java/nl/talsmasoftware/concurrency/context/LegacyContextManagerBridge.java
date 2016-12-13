package nl.talsmasoftware.concurrency.context;

import java.util.*;
import java.util.logging.Level;
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

    /**
     * Service locator for registered legacy {@link ContextManager} implementations.
     */
    private static final ServiceLoader<ContextManager> LEGACY_LOCATOR =
            ServiceLoader.load(ContextManager.class, LegacyContextManagerBridge.class.getClassLoader());

    private static ContextSnapshot createLegacyContextSnapshot() {
        final Map<ContextManager, Object> snapshot = new IdentityHashMap<ContextManager, Object>();
        boolean empty = true;
        for (ContextManager manager : LEGACY_LOCATOR) {
            empty = false;
            final nl.talsmasoftware.concurrency.context.Context activeContext = manager.getActiveContext();
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
