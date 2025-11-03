/*
 * Copyright 2016-2025 Talsma ICT
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
package nl.talsmasoftware.context.api;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({
        "rawtypes",  // We deal with all context manager types here.
        "java:S3416" // As a 'hidden' class within the api, we apply log operations to the relevant interfaces.
})
final class ContextSnapshotImpl implements ContextSnapshot {
    private static final Logger SNAPSHOT_LOGGER = Logger.getLogger(ContextSnapshot.class.getName());
    private static final Logger MANAGER_LOGGER = Logger.getLogger(ContextManager.class.getName());
    private static final Logger TIMER_LOGGER = Logger.getLogger(ContextTimer.class.getName());

    private final List<ContextManager> managers;
    private final Object[] values;

    static ContextSnapshot capture() {
        final long start = System.nanoTime();
        RuntimeException error = null;
        try {
            return new ContextSnapshotImpl();
        } catch (RuntimeException e) {
            error = e;
            SNAPSHOT_LOGGER.log(Level.FINEST, e, () -> "Error capturing ContextSnapshot from " + Thread.currentThread().getName() + ": " + e.getMessage());
            ServiceCache.clear();
            throw e;
        } finally {
            timed(System.nanoTime() - start, ContextSnapshot.class, "capture", error);
        }
    }

    private ContextSnapshotImpl() {
        managers = ServiceCache.cached(ContextManager.class); // Cached list is immutable
        values = new Object[managers.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = getActiveContextValue(managers.get(i));
        }
        if (managers.isEmpty() && SNAPSHOT_LOGGER.isLoggable(Level.FINER)) {
            final Thread currentThread = Thread.currentThread();
            SNAPSHOT_LOGGER.finer(this + " was created but no context managers were found! Thread="
                    + currentThread.getName() + ", ContextClassLoader=" + currentThread.getContextClassLoader());
        }
    }

    public Reactivation reactivate() {
        final long start = System.nanoTime();
        RuntimeException error = null;
        final Context[] reactivatedContexts = new Context[managers.size()];
        try {
            for (int i = 0; i < values.length; i++) {
                reactivatedContexts[i] = reactivate(managers.get(i), values[i]);
            }
        } catch (RuntimeException reactivationException) {
            error = reactivationException;
            tryClose(reactivatedContexts, reactivationException);
            ServiceCache.clear();
            throw reactivationException;
        } finally {
            timed(System.nanoTime() - start, ContextSnapshot.class, "reactivate", error);
        }
        return new ReactivationImpl(reactivatedContexts);
    }

    @Override
    public String toString() {
        return "ContextSnapshot{size=" + managers.size() + '}';
    }

    /**
     * Clears all active contexts from the current thread.
     *
     * @see ContextManager#clearAll()
     */
    static void clearAll() {
        final long start = System.nanoTime();
        for (ContextManager<?> manager : ServiceCache.cached(ContextManager.class)) {
            clear(manager);
        }
        timed(System.nanoTime() - start, ContextManager.class, "clearAll", null);
    }

    /**
     * Implementation of the reactivated 'container' context that closes all reactivated contexts
     * when it is closed itself.<br>
     * This context contains no meaningful value in itself and purely exists to close the reactivated contexts.
     */
    private static final class ReactivationImpl implements Reactivation {
        private final Context[] reactivated;

        private ReactivationImpl(Context[] reactivated) {
            this.reactivated = reactivated;
        }

        public void close() {
            RuntimeException closeException = null;
            // close in reverse order of reactivation
            for (int i = reactivated.length - 1; i >= 0; i--) {
                if (reactivated[i] != null) {
                    try {
                        reactivated[i].close();
                    } catch (RuntimeException rte) {
                        if (closeException == null) closeException = rte;
                        else closeException.addSuppressed(rte);
                    }
                }
            }
            if (closeException != null) {
                throw closeException;
            }
        }

        @Override
        public String toString() {
            return "ContextSnapshot.Reactivation{size=" + reactivated.length + '}';
        }
    }

    private static Object getActiveContextValue(final ContextManager<?> manager) {
        final long start = System.nanoTime();
        RuntimeException error = null;
        try {

            final Object activeContextValue = manager.getActiveContextValue();
            SNAPSHOT_LOGGER.finest(() -> activeContextValue == null
                    ? "There is no active context value for " + manager + " (in thread " + Thread.currentThread().getName() + ")."
                    : "Active context value of " + manager + " in " + Thread.currentThread().getName() + ": " + activeContextValue);
            return activeContextValue;

        } catch (RuntimeException e) {
            SNAPSHOT_LOGGER.log(Level.WARNING, e, () -> "Error obtaining active context from " + manager + " (in thread " + Thread.currentThread().getName() + ").");
            error = e;
            return null;
        } finally {
            timed(System.nanoTime() - start, manager.getClass(), "getActiveContextValue", error);
        }
    }

    private static void clear(ContextManager<?> manager) {
        final long start = System.nanoTime();
        RuntimeException error = null;
        try {

            manager.clear();
            MANAGER_LOGGER.finest(() -> "Active context of " + manager + " was cleared.");

        } catch (RuntimeException e) {
            MANAGER_LOGGER.log(Level.WARNING, e, () -> "Error clearing active context from " + manager + "(in thread " + Thread.currentThread().getName() + ").");
            error = e;
        } finally {
            timed(System.nanoTime() - start, manager.getClass(), "clear", error);
        }
    }

    /**
     * Reactivates a snapshot value for a single context manager.
     *
     * <p>
     * This activates a new context containing the snapshot value with the context manager
     * (normally on another thread the snapshot value was captured from).
     *
     * @param contextManager The context manager to reactivate the snapshot value for.
     * @param snapshotValue  The snapshot value to be reactivated.
     * @return The context to be included in the reactivation object.
     */
    @SuppressWarnings("unchecked") // We get the snapshotValue from the manager itself.
    private static Context reactivate(ContextManager contextManager, Object snapshotValue) {
        long start = System.nanoTime();
        RuntimeException error = null;
        try {

            Context reactivated = contextManager.activate(snapshotValue);
            SNAPSHOT_LOGGER.finest(() -> "Context reactivated from snapshot by " + contextManager + ": " + reactivated + ".");
            return reactivated;

        } catch (RuntimeException e) {
            error = e;
            throw e;
        } finally {
            timed(System.nanoTime() - start, contextManager.getClass(), "activate", error);
        }
    }

    /**
     * Try to close already-reactivated contexts when a later context manager threw an exception.
     *
     * @param reactivatedContexts The contexts that were already reactivated when the error happened.
     * @param reason              The error that happened.
     */
    private static void tryClose(Context[] reactivatedContexts, Throwable reason) {
        for (Context alreadyReactivated : reactivatedContexts) {
            if (alreadyReactivated != null) {
                try {
                    alreadyReactivated.close();
                } catch (RuntimeException rte) {
                    reason.addSuppressed(rte);
                }
            }
        }
    }

    private static void timed(long durationNanos, Class<?> type, String method, Throwable error) {
        for (ContextTimer delegate : ServiceCache.cached(ContextTimer.class)) {
            delegate.update(type, method, durationNanos, TimeUnit.NANOSECONDS, error);
        }
        if (TIMER_LOGGER.isLoggable(Level.FINEST)) {
            TIMER_LOGGER.log(Level.FINEST, "{0}.{1}: {2,number}ns", new Object[]{type.getName(), method, durationNanos});
        }
    }
}
