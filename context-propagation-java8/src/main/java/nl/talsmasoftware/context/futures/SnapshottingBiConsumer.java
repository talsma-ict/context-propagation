package nl.talsmasoftware.context.futures;

import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.delegation.Wrapper;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class SnapshottingBiConsumer<T, U> extends Wrapper<BiConsumer<T, U>> implements BiConsumer<T, U> {
    private final Consumer<ContextSnapshot> snapshotConsumer;

    SnapshottingBiConsumer(BiConsumer<T, U> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(delegate);
        this.snapshotConsumer = snapshotConsumer;
    }

    @Override
    public void accept(T t, U u) {
        try {
            nonNullDelegate().accept(t, u);
        } finally {
            snapshotConsumer.accept(ContextManagers.createContextSnapshot());
        }
    }

}
