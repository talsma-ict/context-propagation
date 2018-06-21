package nl.talsmasoftware.context.futures;

import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.delegation.Wrapper;

import java.util.function.Consumer;
import java.util.function.Function;

final class SnapshottingFunction<T, U> extends Wrapper<Function<T, U>> implements Function<T, U> {
    private final Consumer<ContextSnapshot> snapshotConsumer;

    SnapshottingFunction(Function<T, U> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(delegate);
        this.snapshotConsumer = snapshotConsumer;
    }

    @Override
    public U apply(T t) {
        try {
            return nonNullDelegate().apply(t);
        } finally {
            snapshotConsumer.accept(ContextManagers.createContextSnapshot());
        }
    }
}
