package nl.talsmasoftware.context.futures;

import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.delegation.Wrapper;

import java.util.function.BiFunction;
import java.util.function.Consumer;

final class SnapshottingBiFunction<T, U, R> extends Wrapper<BiFunction<T, U, R>> implements BiFunction<T, U, R> {
    private final Consumer<ContextSnapshot> snapshotConsumer;

    SnapshottingBiFunction(BiFunction<T, U, R> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(delegate);
        this.snapshotConsumer = snapshotConsumer;
    }

    @Override
    public R apply(T t, U u) {
        try {
            return nonNullDelegate().apply(t, u);
        } finally {
            snapshotConsumer.accept(ContextManagers.createContextSnapshot());
        }
    }

}
