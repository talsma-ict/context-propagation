package nl.talsmasoftware.concurrency.context;

import static java.util.Objects.requireNonNull;

/**
 * @author Sjoerd Talsma
 * @deprecated This only exists to allows old ContextManagers to delegate to new implementations.
 */
public class LegacyContext<T> implements Context<T>, nl.talsmasoftware.context.Context<T> {

    private final nl.talsmasoftware.context.Context<T> wrapped;

    protected LegacyContext(nl.talsmasoftware.context.Context<T> wrapped) {
        this.wrapped = requireNonNull(wrapped, "Wrapped context is <null>.");
    }

    public static <T> Context<T> wrap(nl.talsmasoftware.context.Context<T> newContext) {
        return newContext == null || newContext instanceof Context
                ? (Context<T>) newContext : new LegacyContext<>(newContext);
    }

    @Override
    public T getValue() {
        return wrapped.getValue();
    }

    @Override
    public void close() {
        wrapped.close();
    }

    @Override
    public String toString() {
        return "LegacyContext{" + wrapped + '}';
    }

}
