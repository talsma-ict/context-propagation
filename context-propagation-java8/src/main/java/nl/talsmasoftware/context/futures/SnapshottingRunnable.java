package nl.talsmasoftware.context.futures;

import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.delegation.Wrapper;

import java.util.function.Consumer;

final class SnapshottingRunnable extends Wrapper<Runnable> implements Runnable {
    private final Consumer<ContextSnapshot> snapshotConsumer;

    SnapshottingRunnable(Runnable delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(delegate);
        this.snapshotConsumer = snapshotConsumer;
    }

    @Override
    public void run() {
        try {
            nonNullDelegate().run();
        } finally {
            snapshotConsumer.accept(ContextManagers.createContextSnapshot());
        }
    }
}
