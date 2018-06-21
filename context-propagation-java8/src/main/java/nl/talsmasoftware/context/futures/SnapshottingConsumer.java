package nl.talsmasoftware.context.futures;

import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.delegation.Wrapper;

import java.util.function.Consumer;

final class SnapshottingConsumer<T> extends Wrapper<Consumer<T>> implements Consumer<T> {
    private final Consumer<ContextSnapshot> snapshotConsumer;

    SnapshottingConsumer(Consumer<T> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(delegate);
        this.snapshotConsumer = snapshotConsumer;
    }

    @Override
    public void accept(T t) {
        try {
            nonNullDelegate().accept(t);
        } finally {
            snapshotConsumer.accept(ContextManagers.createContextSnapshot());
        }
    }

}
