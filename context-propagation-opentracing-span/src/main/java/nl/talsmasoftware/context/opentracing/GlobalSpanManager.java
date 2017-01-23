package nl.talsmasoftware.context.opentracing;

import io.opentracing.Span;
import io.opentracing.contrib.spanmanager.DefaultSpanManager;
import io.opentracing.contrib.spanmanager.SpanManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sjoerd Talsma
 * @deprecated This class should be stabilized and donated to io.opentracing.contrib somewhere.
 */
final class GlobalSpanManager implements SpanManager {
    private static final Logger LOGGER = Logger.getLogger(GlobalSpanManager.class.getName());

    /**
     * Singleton instance.
     * <p>
     * Since we cannot prevent people using {@linkplain #get() GlobalSpanManager.get()} as a constant,
     * this guarantees that references obtained before, during or after initialization
     * all behave as if obtained <em>after</em> initialization once properly initialized.<br>
     * As a minor additional benefit it makes it harder to circumvent the {@link SpanManager} API.
     */
    private static final GlobalSpanManager INSTANCE = new GlobalSpanManager();

    private static final List<SpanManager> DELEGATES = new CopyOnWriteArrayList<SpanManager>();
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private GlobalSpanManager() {
    }

    public static SpanManager get() {
        return INSTANCE;
    }

    private static void init() {
        if (INITIALIZED.compareAndSet(false, true)) {
            // Use the ServiceLoader to find the declared SpanManager implementations.
            for (SpanManager spanManager : ServiceLoader.load(SpanManager.class, SpanManager.class.getClassLoader())) {
                DELEGATES.add(spanManager);
            }
            DELEGATES.add(DefaultSpanManager.getInstance());
        }
    }

    public static void register(final SpanManager spanManager) {
        init();
        if (!DELEGATES.contains(spanManager)) DELEGATES.add(0, spanManager);
    }

    @Override
    public Span currentSpan() {
        init();
        return DELEGATES.get(0).currentSpan();
    }

    @Override
    public ManagedSpan manage(Span span) {
        init();
        final int delegateCount = DELEGATES.size();
        if (delegateCount == 1) return DELEGATES.get(0).manage(span);
        List<ManagedSpan> managedSpans = new ArrayList<ManagedSpan>(delegateCount);
        for (SpanManager delegate : DELEGATES) {
            try {
                managedSpans.add(delegate.manage(span));
            } catch (RuntimeException manageEx) {
                LOGGER.log(Level.SEVERE, "Error managing " + span + ": " + manageEx.getMessage(), manageEx);
            }
        }
        return new MultiManagedSpan(managedSpans);
    }

    @Override
    public void clear() {
        init();
        for (SpanManager delegate : DELEGATES) {
            try {
                delegate.clear();
            } catch (RuntimeException clearEx) {
                LOGGER.log(Level.SEVERE, "Error clearing " + delegate + ": " + clearEx.getMessage(), clearEx);
            }
        }
    }

    private static final class MultiManagedSpan implements ManagedSpan {
        private final ManagedSpan[] delegates;

        private MultiManagedSpan(Collection<ManagedSpan> delegates) {
            this.delegates = delegates.toArray(new ManagedSpan[delegates.size()]);
        }

        @Override
        public Span getSpan() {
            return delegates[0].getSpan();
        }

        @Override
        public void release() {
            for (ManagedSpan delegate : delegates) {
                try {
                    delegate.release();
                } catch (RuntimeException releaseEx) {
                    LOGGER.log(Level.SEVERE, "Error releasing " + delegate + ": " + releaseEx.getMessage(), releaseEx);
                }
            }
        }

        @Override
        public void close() {
            for (ManagedSpan delegate : delegates) {
                try {
                    delegate.close();
                } catch (RuntimeException closeEx) {
                    LOGGER.log(Level.SEVERE, "Error closing " + delegate + ": " + closeEx.getMessage(), closeEx);
                }
            }
        }
    }

}
