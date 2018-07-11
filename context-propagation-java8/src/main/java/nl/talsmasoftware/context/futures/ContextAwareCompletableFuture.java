/*
 * Copyright 2016-2018 Talsma ICT
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
package nl.talsmasoftware.context.futures;

import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.functions.BiConsumerWithContext;
import nl.talsmasoftware.context.functions.BiFunctionWithContext;
import nl.talsmasoftware.context.functions.ConsumerWithContext;
import nl.talsmasoftware.context.functions.FunctionWithContext;
import nl.talsmasoftware.context.functions.RunnableWithContext;
import nl.talsmasoftware.context.functions.SupplierWithContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * This class extends the standard {@link CompletableFuture} that was introduced in java version 8.
 * <p>
 * The class is a 'normal' Completable Future, but every successive call made on the result will be made within the
 * {@link ContextSnapshot context during creation} of this {@link ContextAwareCompletableFuture}.
 *
 * @author Sjoerd Talsma
 */
public class ContextAwareCompletableFuture<T> extends CompletableFuture<T> {

    private final ContextSnapshotHolder snapshotHolder;

    /**
     * Creates a new {@link ContextSnapshot} and remembers that in this completable future, running all
     * completion methods within this snapshot.
     *
     * @see ContextManagers#createContextSnapshot()
     */
    public ContextAwareCompletableFuture() {
        this((ContextSnapshot) null);
    }

    /**
     * Creates a new {@link CompletableFuture} where all completion methods are run within the specified
     * snapshot context.
     *
     * @param snapshot The snapshot to run completion methods in (or specify <code>null</code> to take a
     *                 new snapshot upon creation of this completable future).
     * @see ContextManagers#createContextSnapshot()
     */
    public ContextAwareCompletableFuture(ContextSnapshot snapshot) {
        this(new ContextSnapshotHolder(snapshot));
    }

    private ContextAwareCompletableFuture(ContextSnapshotHolder holder) {
        this.snapshotHolder = requireNonNull(holder, "Snapshot holder is <null>");
    }

    /**
     * Runs the {@code supplier} task in the common {@link java.util.concurrent.ForkJoinPool ForkJoinPool}
     * <em>within the current context</em> and also applies that context to all successive
     * calls to the {@code CompletableFuture}.
     *
     * @param supplier a function returning the value to be used to complete the returned CompletableFuture
     * @param <U>      the function's return type
     * @return the new CompletableFuture that propagates a snapshot of the current context
     * @see CompletableFuture#supplyAsync(Supplier)
     * @see ContextAwareCompletableFuture#supplyAsync(Supplier, Executor, ContextSnapshot)
     */
    public static <U> ContextAwareCompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return supplyAsync(supplier, null, null);
    }

    /**
     * Runs the {@code supplier} task in the specified {@link Executor executor}
     * <em>within the current context</em> and also applies that context to all successive
     * calls to the {@code CompletableFuture}.
     *
     * @param supplier a function returning the value to be used to complete the returned CompletableFuture
     * @param executor the executor to use for asynchronous execution
     * @param <U>      the function's return type
     * @return the new CompletableFuture that propagates a snapshot of the current context
     * @see CompletableFuture#supplyAsync(Supplier, Executor)
     * @see ContextAwareCompletableFuture#supplyAsync(Supplier, Executor, ContextSnapshot)
     */
    public static <U> ContextAwareCompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
        return supplyAsync(supplier, executor, null);
    }

    /**
     * Runs the {@code supplier} task in the specified {@link Executor executor}
     * <em>within the specified {@link ContextSnapshot context snapshot}</em> and also applies that context
     * to all successive calls to the {@code CompletableFuture}.
     * <p>
     * This method is lenient to {@code null} values for {@code executor} and {@code snapshot}:<br>
     * If {@code executor == null} the common {@link java.util.concurrent.ForkJoinPool ForkJoinPool} is used as
     * specified by {@link CompletableFuture#supplyAsync(Supplier)}.<br>
     * If {@code snapshot == null} a {@link ContextManagers#createContextSnapshot() new context snapshot} is
     * created for all successive calls to the {@code CompletableFuture} and the {@link Supplier}
     * (if not already a {@link SupplierWithContext}).
     *
     * @param supplier a function returning the value to be used to complete the returned CompletableFuture
     * @param executor the executor to use for asynchronous execution
     * @param snapshot a snapshot of the context to be propagated in the supplier function
     *                 and all successive calls of this completable future
     * @param <U>      the function's return type
     * @return the new CompletableFuture that propagates the specified context snapshot
     * @see CompletableFuture#supplyAsync(Supplier, Executor)
     */
    public static <U> ContextAwareCompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor, ContextSnapshot snapshot) {
        ContextSnapshotHolder holder = new ContextSnapshotHolder(snapshot);
        supplier = new SupplierWithContext<U>(holder, supplier, holder) {
        };
        return wrap(executor == null
                        ? CompletableFuture.supplyAsync(supplier)
                        : CompletableFuture.supplyAsync(supplier, executor),
                holder);
    }

    /**
     * Runs the {@code runnable} task in the common {@link java.util.concurrent.ForkJoinPool ForkJoinPool}
     * <em>within the current context</em> and also applies that context to all successive
     * calls to the {@code CompletableFuture}.
     *
     * @param runnable the action to run before completing the returned CompletableFuture
     * @return the new CompletableFuture that propagates a snapshot of the current context
     * @see CompletableFuture#runAsync(Runnable)
     * @see ContextAwareCompletableFuture#runAsync(Runnable, Executor, ContextSnapshot)
     */
    public static ContextAwareCompletableFuture<Void> runAsync(Runnable runnable) {
        return runAsync(runnable, null, null);
    }

    /**
     * Runs the {@code runnable} task in the specified {@link Executor executor}
     * <em>within the current context</em> and also applies that context to all successive
     * calls to the {@code CompletableFuture}.
     *
     * @param runnable the action to run before completing the returned CompletableFuture
     * @param executor the executor to use for asynchronous execution
     * @return the new CompletableFuture that propagates a snapshot of the current context
     * @see CompletableFuture#runAsync(Runnable, Executor)
     * @see ContextAwareCompletableFuture#runAsync(Runnable, Executor, ContextSnapshot)
     */
    public static ContextAwareCompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        return runAsync(runnable, executor, null);
    }

    /**
     * Runs the {@code runnable} task in the specified {@link Executor executor}
     * <em>within the specified {@link ContextSnapshot context snapshot}</em> and also applies that context
     * to all successive calls to the {@code CompletableFuture}.
     * <p>
     * This method is lenient to {@code null} values for {@code executor} and {@code snapshot}:<br>
     * If {@code executor == null} the common {@link java.util.concurrent.ForkJoinPool ForkJoinPool} is used as
     * specified by {@link CompletableFuture#supplyAsync(Supplier)}.<br>
     * If {@code snapshot == null} a {@link ContextManagers#createContextSnapshot() new context snapshot} is
     * created for all successive calls to the {@code CompletableFuture} and the {@link Supplier}
     * (if not already a {@link SupplierWithContext}).
     *
     * @param runnable the action to run before completing the returned CompletableFuture
     * @param executor the executor to use for asynchronous execution
     * @return the new CompletableFuture that propagates a snapshot of the current context
     * @see CompletableFuture#runAsync(Runnable, Executor)
     */
    public static ContextAwareCompletableFuture<Void> runAsync(Runnable runnable, Executor executor, ContextSnapshot snapshot) {
        final ContextSnapshotHolder holder = new ContextSnapshotHolder(snapshot);
        runnable = new RunnableWithContext(holder, runnable, holder) {
        };
        return wrap(executor == null
                        ? CompletableFuture.runAsync(runnable)
                        : CompletableFuture.runAsync(runnable, executor),
                holder);
    }

    private static <U> ContextAwareCompletableFuture<U> wrap(CompletableFuture<U> completableFuture, ContextSnapshotHolder holder) {
        ContextAwareCompletableFuture<U> contextAwareCompletableFuture = new ContextAwareCompletableFuture<>(holder);
        completableFuture.whenComplete((result, throwable) -> {
            if (throwable != null) contextAwareCompletableFuture.completeExceptionally(throwable);
            else contextAwareCompletableFuture.complete(result);
        });
        return contextAwareCompletableFuture;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return wrap(super.thenApply(new FunctionWithContext(snapshotHolder, fn, snapshotHolder) {
        }), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return wrap(super.thenApplyAsync(new FunctionWithContext(snapshotHolder, fn, snapshotHolder) {
        }), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return wrap(super.thenApplyAsync(new FunctionWithContext(snapshotHolder, fn, snapshotHolder) {
        }, executor), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return wrap(super.thenAccept(new ConsumerWithContext(snapshotHolder, action, snapshotHolder) {
        }), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return wrap(super.thenAcceptAsync(new ConsumerWithContext(snapshotHolder, action, snapshotHolder) {
        }), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return wrap(super.thenAcceptAsync(new ConsumerWithContext(snapshotHolder, action, snapshotHolder) {
        }, executor), snapshotHolder);
    }

    @Override
    public CompletableFuture<Void> thenRun(Runnable action) {
        return wrap(super.thenRun(new RunnableWithContext(snapshotHolder, action, snapshotHolder) {
        }), snapshotHolder);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return wrap(super.thenRunAsync(new RunnableWithContext(snapshotHolder, action, snapshotHolder) {
        }), snapshotHolder);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return wrap(super.thenRunAsync(new RunnableWithContext(snapshotHolder, action, snapshotHolder) {
        }, executor), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return wrap(super.thenCombine(other, new BiFunctionWithContext(snapshotHolder, fn, null) {
        }), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return wrap(super.thenCombineAsync(other, new BiFunctionWithContext(snapshotHolder, fn, null) {
        }), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U, V> CompletableFuture<V> thenCombineAsync(
            CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return wrap(super.thenCombineAsync(other, new BiFunctionWithContext(snapshotHolder, fn, null) {
        }, executor), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return wrap(super.thenAcceptBoth(other, new BiConsumerWithContext(snapshotHolder, action, null) {
        }), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return super.thenAcceptBothAsync(other, new BiConsumerWithContext(snapshotHolder, action, null) {
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor) {
        return wrap(super.thenAcceptBothAsync(other, new BiConsumerWithContext(snapshotHolder, action, null) {
        }, executor), snapshotHolder);
    }

    @Override
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return wrap(super.runAfterBoth(other, new RunnableWithContext(snapshotHolder, action, snapshotHolder) {
        }), snapshotHolder);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return wrap(super.runAfterBothAsync(other, new RunnableWithContext(snapshotHolder, action, snapshotHolder) {
        }), snapshotHolder);
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return wrap(super.runAfterBothAsync(other, new RunnableWithContext(snapshotHolder, action, snapshotHolder) {
        }, executor), snapshotHolder);
    }

    @Override
    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        final ContextSnapshot snapshot = snapshotHolder.get();
        return wrap(super.applyToEither(other, new FunctionWithContext<>(snapshot, fn)), snapshotHolder);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        final ContextSnapshot snapshot = snapshotHolder.get();
        return wrap(super.applyToEitherAsync(other, new FunctionWithContext<>(snapshot, fn)), snapshotHolder);
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(
            CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor) {
        final ContextSnapshot snapshot = snapshotHolder.get();
        return wrap(super.applyToEitherAsync(other, new FunctionWithContext<>(snapshot, fn), executor), snapshotHolder);
    }

    @Override
    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        final ContextSnapshot snapshot = snapshotHolder.get();
        return wrap(super.acceptEither(other, new ConsumerWithContext<>(snapshot, action)), snapshotHolder);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        final ContextSnapshot snapshot = snapshotHolder.get();
        return wrap(super.acceptEitherAsync(other, new ConsumerWithContext<>(snapshot, action)), snapshotHolder);
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(
            CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor) {
        final ContextSnapshot snapshot = snapshotHolder.get();
        return wrap(super.acceptEitherAsync(other, new ConsumerWithContext<>(snapshot, action), executor), snapshotHolder);
    }

    @Override
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        final ContextSnapshot snapshot = snapshotHolder.get();
        return wrap(super.runAfterEither(other, new RunnableWithContext(snapshot, action)), snapshotHolder);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        final ContextSnapshot snapshot = snapshotHolder.get();
        return wrap(super.runAfterEitherAsync(other, new RunnableWithContext(snapshot, action)), snapshotHolder);
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        final ContextSnapshot snapshot = snapshotHolder.get();
        return wrap(super.runAfterEitherAsync(other, new RunnableWithContext(snapshot, action), executor), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return wrap(super.thenCompose(new FunctionWithContext(snapshotHolder, fn, snapshotHolder) {
        }), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return wrap(super.thenComposeAsync(new FunctionWithContext(snapshotHolder, fn, snapshotHolder) {
        }), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        return wrap(super.thenComposeAsync(new FunctionWithContext(snapshotHolder, fn, snapshotHolder) {
        }, executor), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return wrap(super.whenComplete(new BiConsumerWithContext(snapshotHolder, action, snapshotHolder) {
        }), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return wrap(super.whenCompleteAsync(new BiConsumerWithContext(snapshotHolder, action, snapshotHolder) {
        }), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return wrap(super.whenCompleteAsync(new BiConsumerWithContext(snapshotHolder, action, snapshotHolder) {
        }, executor), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return wrap(super.handle(new BiFunctionWithContext(snapshotHolder, fn, snapshotHolder) {
        }), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return wrap(super.handleAsync(new BiFunctionWithContext(snapshotHolder, fn, snapshotHolder) {
        }), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return wrap(super.handleAsync(new BiFunctionWithContext(snapshotHolder, fn, snapshotHolder) {
        }, executor), snapshotHolder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return wrap(super.exceptionally(new FunctionWithContext(snapshotHolder, fn, snapshotHolder) {
        }), snapshotHolder);
    }

}
