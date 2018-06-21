package nl.talsmasoftware.context.futures;

import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.delegation.Wrapper;

import java.util.function.Consumer;
import java.util.function.Supplier;

final class SnapshottingSupplier<T> extends Wrapper<Supplier<T>> implements Supplier<T> {
    private final Consumer<ContextSnapshot> snapshotConsumer;

    SnapshottingSupplier(Supplier<T> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(delegate);
        this.snapshotConsumer = snapshotConsumer;
    }

    @Override
    public T get() {
        try {
            return nonNullDelegate().get();
        } finally {
            snapshotConsumer.accept(ContextManagers.createContextSnapshot());
        }
    }
}
