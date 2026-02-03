/*
 * Copyright 2016-2026 Talsma ICT
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
package nl.talsmasoftware.context.core.concurrent;

import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.core.function.BiConsumerWithContext;
import nl.talsmasoftware.context.core.function.BiFunctionWithContext;
import nl.talsmasoftware.context.core.function.ConsumerWithContext;
import nl.talsmasoftware.context.core.function.FunctionWithContext;
import nl.talsmasoftware.context.core.function.RunnableWithContext;
import nl.talsmasoftware.context.core.function.SupplierWithContext;

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
 * {@linkplain CompletableFuture} that makes every successive call with a reactivated
 * {@linkplain ContextSnapshot context snapshot} captured from the submitting thread.
 *
 * <p>
 * Snapshots are properly closed after running the function body.
 *
 * <p>
 * The implementation allows the creator to {@linkplain ContextSnapshot#capture() capture a new snapshot} to propagate
 * after each completion stage. Please be aware that setting {@linkplain #takeNewSnapshot()} may cause relatively high overhead.
 *
 * @param <T> The result type of the future.
 * @author Sjoerd Talsma
 */
public class ContextAwareCompletableFuture<T> extends CompletableFuture<T> {

    /**
     * Holder for context snapshots to be propagated from one CompletionStage to the next.
     */
    private final ContextSnapshotHolder snapshotHolder;

    /**
     * Whether to capture a new snapshot after each completion stage.
     */
    private final boolean captureNewSnapshot;

    /**
     * Captures a new {@link ContextSnapshot} and remembers that in this completable future,
     * running all completion methods within this snapshot.
     *
     * @see ContextSnapshot#capture()
     */
    public ContextAwareCompletableFuture() {
        this(null);
    }

    /**
     * Creates a new {@link CompletableFuture} where all completion methods are run within the specified
     * snapshot context.
     *
     * @param snapshot the snapshot to reactivate for the completion methods (optional, a new snapshot is captured if {@code null}).
     * @see ContextSnapshot#capture()
     */
    public ContextAwareCompletableFuture(ContextSnapshot snapshot) {
        this(new ContextSnapshotHolder(snapshot), false);
    }

    private ContextAwareCompletableFuture(ContextSnapshotHolder holder, boolean captureNewSnapshot) {
        this.snapshotHolder = requireNonNull(holder, "Snapshot holder is <null>");
        this.captureNewSnapshot = captureNewSnapshot;
    }

    /**
     * Runs the {@code supplier} task in the common {@link java.util.concurrent.ForkJoinPool ForkJoinPool}
     * <em>within the current context</em> and also applies that context to all successive
     * calls to the {@code CompletableFuture}.
     *
     * @param supplier a function to be performed asynchronously returning the result of the CompletableFuture
     * @param <U>      the function's return type
     * @return The new CompletableFuture that propagates a snapshot of the current context
     * @see CompletableFuture#supplyAsync(Supplier)
     * @see ContextAwareCompletableFuture#supplyAsync(Supplier, Executor, ContextSnapshot, boolean)
     */
    public static <U> ContextAwareCompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return supplyAsync(supplier, null, null, false);
    }

    /**
     * Runs the {@code supplier} task in the specified {@link Executor executor}
     * <em>within the current context</em> and also applies that context to all successive
     * calls to the {@code CompletableFuture}.
     *
     * @param supplier a function returning the value to be used to complete the returned CompletableFuture
     * @param executor the executor to use for asynchronous execution
     * @param <U>      the function's return type
     * @return The new CompletableFuture that propagates a snapshot of the current context
     * @see CompletableFuture#supplyAsync(Supplier, Executor)
     * @see ContextAwareCompletableFuture#supplyAsync(Supplier, Executor, ContextSnapshot, boolean)
     */
    public static <U> ContextAwareCompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
        return supplyAsync(supplier, executor, null, false);
    }

    /**
     * Runs the {@code supplier} task in the specified {@link Executor executor}
     * <em>within the specified {@link ContextSnapshot context snapshot}</em> and also applies that context
     * to all successive calls to the {@code CompletableFuture}.
     * <p>
     * This method is lenient to {@code null} values for {@code executor} and {@code snapshot}:<br>
     * If {@code executor == null} the common {@link java.util.concurrent.ForkJoinPool ForkJoinPool} is used as
     * specified by {@link CompletableFuture#supplyAsync(Supplier)}.<br>
     * If {@code snapshot == null} a {@link ContextSnapshot#capture() new context snapshot} is
     * captured for the {@link Supplier} (if not already a {@link SupplierWithContext}).
     *
     * @param supplier a function returning the value to be used to complete the returned CompletableFuture
     * @param executor the executor to use for asynchronous execution
     * @param snapshot a snapshot of the context to be propagated in the supplier function
     *                 and all successive calls of this completable future
     * @param <U>      the function's return type
     * @return The new CompletableFuture that propagates the specified context snapshot
     * @see CompletableFuture#supplyAsync(Supplier, Executor)
     * @see ContextAwareCompletableFuture#supplyAsync(Supplier, Executor, ContextSnapshot, boolean)
     */
    public static <U> ContextAwareCompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor, ContextSnapshot snapshot) {
        return supplyAsync(supplier, executor, snapshot, false);
    }

    /**
     * Runs the {@code supplier} task in the specified {@link Executor executor}
     * <em>within the specified {@link ContextSnapshot context snapshot}</em> and also applies that context
     * to all successive calls to the {@code CompletableFuture}.
     * <p>
     * This method is lenient to {@code null} values for {@code executor} and {@code snapshot}:<br>
     * If {@code executor == null} the common {@link java.util.concurrent.ForkJoinPool ForkJoinPool} is used as
     * specified by {@link CompletableFuture#supplyAsync(Supplier)}.<br>
     * If {@code snapshot == null} a {@link ContextSnapshot#capture() new context snapshot} is
     * captured for the {@link Supplier} (if not already a {@link SupplierWithContext}).
     *
     * @param supplier           a function returning the value to be used to complete the returned CompletableFuture
     * @param executor           the executor to use for asynchronous execution
     * @param snapshot           a snapshot of the context to be propagated in the supplier function
     *                           and all successive calls of this completable future
     * @param captureNewSnapshot whether a new ContextSnapshot should be captured after the supplier function is done.
     *                           If {@code false}, the snapshot from the caller propagates to all following completion stages.
     *                           If {@code true}, a new snapshot is captured after each completion stage to propagate into the next.
     * @param <U>                the function's return type
     * @return The new CompletableFuture that propagates the specified context snapshot
     * @see CompletableFuture#supplyAsync(Supplier, Executor)
     * @since 1.0.4
     */
    public static <U> ContextAwareCompletableFuture<U> supplyAsync(
            Supplier<U> supplier, Executor executor, ContextSnapshot snapshot, boolean captureNewSnapshot) {

        final ContextSnapshotHolder holder = new ContextSnapshotHolder(snapshot);
        supplier = new SupplierWithContext<U>(holder, supplier, captureNewSnapshot ? holder : null) {
        };
        return wrap(executor == null
                        ? CompletableFuture.supplyAsync(supplier)
                        : CompletableFuture.supplyAsync(supplier, executor),
                holder,
                captureNewSnapshot);
    }

    /**
     * Runs the {@code runnable} task in the common {@link java.util.concurrent.ForkJoinPool ForkJoinPool}
     * <em>within the current context</em> and also applies that context to all successive
     * calls to the {@code CompletableFuture}.
     *
     * @param runnable the action to run before completing the returned CompletableFuture
     * @return The new CompletableFuture that propagates a snapshot of the current context
     * @see CompletableFuture#runAsync(Runnable)
     * @see ContextAwareCompletableFuture#runAsync(Runnable, Executor, ContextSnapshot, boolean)
     */
    public static ContextAwareCompletableFuture<Void> runAsync(Runnable runnable) {
        return runAsync(runnable, null, null, false);
    }

    /**
     * Runs the {@code runnable} task in the specified {@link Executor executor}
     * <em>within the current context</em> and also applies that context to all successive
     * calls to the {@code CompletableFuture}.
     *
     * @param runnable the action to run before completing the returned CompletableFuture
     * @param executor the executor to use for asynchronous execution
     * @return The new CompletableFuture that propagates a snapshot of the current context
     * @see CompletableFuture#runAsync(Runnable, Executor)
     * @see ContextAwareCompletableFuture#runAsync(Runnable, Executor, ContextSnapshot, boolean)
     */
    public static ContextAwareCompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        return runAsync(runnable, executor, null, false);
    }

    /**
     * Runs the {@code runnable} task in the specified {@link Executor executor}
     * <em>within the specified {@link ContextSnapshot context snapshot}</em> and also applies that context
     * to all successive calls to the {@code CompletableFuture}.
     * <p>
     * This method is lenient to {@code null} values for {@code executor} and {@code snapshot}:<br>
     * If {@code executor == null} the common {@link java.util.concurrent.ForkJoinPool ForkJoinPool} is used as
     * specified by {@link CompletableFuture#supplyAsync(Supplier)}.<br>
     * If {@code snapshot == null} a {@link ContextSnapshot#capture() new context snapshot} is
     * captured for the {@link Supplier} (if not already a {@link SupplierWithContext}).
     *
     * @param runnable the action to run before completing the returned CompletableFuture
     * @param executor the executor to use for asynchronous execution
     * @param snapshot the context snapshot to reactivate in the runnable action
     * @return The new CompletableFuture that propagates a snapshot of the current context
     * @see CompletableFuture#runAsync(Runnable, Executor)
     * @see ContextAwareCompletableFuture#runAsync(Runnable, Executor, ContextSnapshot, boolean)
     */
    public static ContextAwareCompletableFuture<Void> runAsync(Runnable runnable, Executor executor, ContextSnapshot snapshot) {
        return runAsync(runnable, executor, snapshot, false);
    }

    /**
     * Runs the {@code runnable} task in the specified {@link Executor executor}
     * <em>within the specified {@link ContextSnapshot context snapshot}</em> and also applies that context
     * to all successive calls to the {@code CompletableFuture}.
     * <p>
     * This method is lenient to {@code null} values for {@code executor} and {@code snapshot}:<br>
     * If {@code executor == null} the common {@link java.util.concurrent.ForkJoinPool ForkJoinPool} is used as
     * specified by {@link CompletableFuture#supplyAsync(Supplier)}.<br>
     * If {@code snapshot == null} a {@link ContextSnapshot#capture() new context snapshot} is
     * captured for the {@link Supplier} (if not already a {@link SupplierWithContext}).
     *
     * @param runnable           the action to run before completing the returned CompletableFuture
     * @param executor           the executor to use for asynchronous execution
     * @param snapshot           the context snapshot to reactivate in the runnable action
     * @param captureNewSnapshot whether a new ContextSnapshot should be captured after the supplier function is done.
     *                           If {@code false}, the snapshot from the caller propagates to all following completion stages.
     *                           If {@code true}, a new snapshot is captured after each completion stage to propagate into the next.
     * @return The new CompletableFuture that propagates a snapshot of the current context
     * @see CompletableFuture#runAsync(Runnable, Executor)
     * @since 1.0.4
     */
    public static ContextAwareCompletableFuture<Void> runAsync(
            Runnable runnable, Executor executor, ContextSnapshot snapshot, boolean captureNewSnapshot) {

        final ContextSnapshotHolder holder = new ContextSnapshotHolder(snapshot);
        runnable = new RunnableWithContext(holder, runnable, captureNewSnapshot ? holder : null) {
        };
        return wrap(executor == null
                        ? CompletableFuture.runAsync(runnable)
                        : CompletableFuture.runAsync(runnable, executor),
                holder,
                captureNewSnapshot);
    }

    /**
     * Creates a new {@code ContextAwareCompletableFuture} from the already-completed value.
     *
     * <p>
     * A new {@linkplain ContextSnapshot} is captured and reactivated in all
     * following {@linkplain CompletionStage completion stages}.
     *
     * @param value the value to return from the already-completed future.
     * @param <U>   the type of the value
     * @return New {@code ContextAwareCompletableFuture} returning the completed value
     * and containing a new {@code ContextSnapshot}.
     * @see #completedFuture(Object, ContextSnapshot)
     * @since 1.0.5
     */
    public static <U> ContextAwareCompletableFuture<U> completedFuture(U value) {
        return completedFuture(value, null);
    }

    /**
     * Creates a new {@code ContextAwareCompletableFuture} from the already-completed value.
     *
     * <p>
     * The specified {@linkplain ContextSnapshot} is reactivated in all following {@linkplain CompletionStage completion stages}.
     * If the specified snapshot is {@code null}, a new {@linkplain ContextSnapshot} will be captured.
     *
     * @param value    the value to return from the already-completed future.
     * @param snapshot the context snapshot to reactivate in following completion stages
     *                 (optional, specify {@code null} to capture a new snapshot)
     * @param <U>      the type of the value
     * @return New {@code ContextAwareCompletableFuture} for the completed value,
     * containing the specified {@code ContextSnapshot}.
     * @since 1.0.5
     */
    public static <U> ContextAwareCompletableFuture<U> completedFuture(U value, ContextSnapshot snapshot) {
        final ContextAwareCompletableFuture<U> completedFuture = new ContextAwareCompletableFuture<>(snapshot);
        completedFuture.complete(value);
        return completedFuture;
    }

    /**
     * Creates a new {@code CompletionStage} from the already-completed value.
     *
     * <p>
     * A new {@linkplain ContextSnapshot} is captured and reactivated in all
     * following {@linkplain CompletionStage completion stages}.
     *
     * @param value the value to return from the already-completed stage.
     * @param <U>   the type of the value
     * @return New {@code CompletionStage} returning the completed value
     * and containing a new {@code ContextSnapshot}.
     * @see #completedFuture(Object, ContextSnapshot)
     * @since 1.0.5
     */
    public static <U> CompletionStage<U> completedStage(U value) {
        return completedFuture(value, null);
    }

    /**
     * Creates a new {@code ContextAwareCompletableFuture} that is already completed
     * exceptionally with the given exception.
     *
     * <p>
     * A new {@linkplain ContextSnapshot} is captured and reactivated in all
     * following {@linkplain CompletionStage completion stages}.
     *
     * @param ex  the exception
     * @param <U> the type of the value
     * @return New {@code ContextAwareCompletableFuture} throwing the exception
     * and containing a new {@code ContextSnapshot}.
     * @see #failedFuture(Throwable, ContextSnapshot)
     * @since 1.0.5
     */
    public static <U> ContextAwareCompletableFuture<U> failedFuture(Throwable ex) {
        return failedFuture(ex, null);
    }

    /**
     * Creates a new {@code ContextAwareCompletableFuture} that is already completed
     * exceptionally with the given exception.
     *
     * <p>
     * The specified {@code snapshot} is reactivated in all
     * following {@linkplain CompletionStage completion stages}.
     *
     * @param ex       the exception
     * @param snapshot the context snapshot to reactivate in following completion stages
     *                 (optional, specify {@code null} to capture a new snapshot)
     * @param <U>      the type of the value
     * @return New {@code ContextAwareCompletableFuture} throwing the exception
     * and containing the specified {@code snapshot}.
     * @since 1.0.5
     */
    public static <U> ContextAwareCompletableFuture<U> failedFuture(Throwable ex, ContextSnapshot snapshot) {
        final ContextAwareCompletableFuture<U> failedFuture = new ContextAwareCompletableFuture<>(snapshot);
        failedFuture.completeExceptionally(ex);
        return failedFuture;
    }

    /**
     * Creates a new {@code CompletionStage} that is already completed
     * exceptionally with the given exception.
     *
     * <p>
     * A new {@linkplain ContextSnapshot} is captured and reactivated in all
     * following {@linkplain CompletionStage completion stages}.
     *
     * @param ex  the exception
     * @param <U> the type of the value
     * @return New {@code CompletionStage} throwing the exception
     * and containing a new {@code ContextSnapshot}.
     * @see #failedFuture(Throwable, ContextSnapshot)
     * @since 1.0.5
     */
    public static <U> CompletionStage<U> failedStage(Throwable ex) {
        return failedFuture(ex, null);
    }

    /**
     * Returns a new CompletableFuture that is completed when all
     * the given CompletableFutures complete.  If any of the given
     * CompletableFutures complete exceptionally, then the returned
     * CompletableFuture also does so, with a CompletionException
     * holding this exception as its cause.  Otherwise, the results,
     * if any, of the given CompletableFutures are not reflected in
     * the returned CompletableFuture, but may be obtained by
     * inspecting them individually. If no CompletableFutures are
     * provided, returns a CompletableFuture completed with the value
     * {@code null}.
     *
     * <p>
     * Among the applications of this method is to await completion
     * of a set of independent CompletableFutures before continuing a
     * program, as in: {@code CompletableFuture.allOf(c1, c2,
     * c3).join();}.
     *
     * <p>
     * A new {@linkplain ContextSnapshot} is captured and reactivated in all
     * following {@linkplain CompletionStage completion stages}.
     *
     * @param cfs the CompletableFutures
     * @return A new {@code ContextAwareCompletableFuture} that is completed when all the given CompletableFutures complete
     * @throws NullPointerException if the array or any of its elements are {@code null}
     * @since 1.0.5
     */
    public static ContextAwareCompletableFuture<Void> allOf(CompletableFuture<?>... cfs) {
        return allOf(null, cfs);
    }

    /**
     * Returns a new CompletableFuture that is completed when all
     * the given CompletableFutures complete.  If any of the given
     * CompletableFutures complete exceptionally, then the returned
     * CompletableFuture also does so, with a CompletionException
     * holding this exception as its cause.  Otherwise, the results,
     * if any, of the given CompletableFutures are not reflected in
     * the returned CompletableFuture, but may be obtained by
     * inspecting them individually. If no CompletableFutures are
     * provided, returns a CompletableFuture completed with the value
     * {@code null}.
     *
     * <p>
     * Among the applications of this method is to await completion
     * of a set of independent CompletableFutures before continuing a
     * program, as in: {@code CompletableFuture.allOf(c1, c2,
     * c3).join();}.
     *
     * <p>
     * The specified {@linkplain ContextSnapshot} is reactivated in all
     * following {@linkplain CompletionStage completion stages}.
     *
     * @param snapshot the context snapshot to reactivate in following completion stages
     *                 (optional, specify {@code null} to capture a new snapshot)
     * @param cfs      the CompletableFutures
     * @return A new {@code ContextAwareCompletableFuture} that is completed when all the CompletableFutures complete
     * @throws NullPointerException if the array or any of its elements are {@code null}
     * @since 1.0.5
     */
    public static ContextAwareCompletableFuture<Void> allOf(ContextSnapshot snapshot, CompletableFuture<?>... cfs) {
        final ContextSnapshotHolder holder = new ContextSnapshotHolder(snapshot);
        return wrap(CompletableFuture.allOf(cfs), holder, false);
    }

    /**
     * Returns a new CompletableFuture that is completed when any of
     * the given CompletableFutures complete, with the same result.
     * Otherwise, if it completed exceptionally, the returned
     * CompletableFuture also does so, with a CompletionException
     * holding this exception as its cause.  If no CompletableFutures
     * are provided, an incomplete CompletableFuture is returned.
     *
     * <p>
     * A new {@linkplain ContextSnapshot} is captured and reactivated in all
     * following {@linkplain CompletionStage completion stages}.
     *
     * @param cfs the CompletableFutures
     * @return a new CompletableFuture that is completed with the result or Exception
     * from any given CompletableFutures when one completes.
     * @throws NullPointerException if the array or any of its elements are {@code null}
     * @since 1.0.5
     */
    public static ContextAwareCompletableFuture<Object> anyOf(CompletableFuture<?>... cfs) {
        return anyOf(null, cfs);
    }

    /**
     * Returns a new CompletableFuture that is completed when any of
     * the given CompletableFutures complete, with the same result.
     * Otherwise, if it completed exceptionally, the returned
     * CompletableFuture also does so, with a CompletionException
     * holding this exception as its cause.  If no CompletableFutures
     * are provided, an incomplete CompletableFuture is returned.
     *
     * <p>
     * The specified {@linkplain ContextSnapshot} is reactivated in all
     * following {@linkplain CompletionStage completion stages}.
     *
     * @param snapshot the context snapshot to reactivate in following completion stages
     *                 (optional, specify {@code null} to capture a new snapshot)
     * @param cfs      the CompletableFutures
     * @return a new CompletableFuture that is completed with the result or Exception
     * from any given CompletableFutures when one completes.
     * @throws NullPointerException if the array or any of its elements are {@code null}
     * @since 1.0.5
     */
    public static ContextAwareCompletableFuture<Object> anyOf(ContextSnapshot snapshot, CompletableFuture<?>... cfs) {
        final ContextSnapshotHolder holder = new ContextSnapshotHolder(snapshot);
        return wrap(CompletableFuture.anyOf(cfs), holder, false);
    }

    private static <U> ContextAwareCompletableFuture<U> wrap(CompletableFuture<U> completableFuture, ContextSnapshotHolder holder, boolean captureNewSnapshot) {
        ContextAwareCompletableFuture<U> contextAwareCompletableFuture = new ContextAwareCompletableFuture<>(holder, captureNewSnapshot);
        completableFuture.whenComplete((result, throwable) -> {
            if (throwable != null) contextAwareCompletableFuture.completeExceptionally(throwable);
            else contextAwareCompletableFuture.complete(result);
        });
        return contextAwareCompletableFuture;
    }

    /**
     * Returns the snapshot holder as a result snapshot consumer to indicate that a new snapshot must be captured after
     * each completion stage. Alternatively, returns {@code null} if no new snapshot should be captured.
     *
     * @return The {@code snapshotHolder} if {@code captureNewSnapshot == true} or otherwise {@code null}.
     */
    private Consumer<ContextSnapshot> resultSnapshotConsumer() {
        return captureNewSnapshot ? snapshotHolder : null;
    }

    /**
     * Returns a context-aware CompletableFuture that captures a new snapshot after each completion stage.
     *
     * <p>
     * This means that after each {@code then...}, {@code run...}, {@code apply...} method,
     * after calling the function, <strong>a new context snapshot is captured</strong> for follow-up calls.
     *
     * <p>
     * Only use this when chaining completable futures where the completion stages may update contextual values.<br>
     * <strong>Warning:</strong> <em>This may result in unnecessary context snapshots being captured.</em>
     *
     * @return A new context-aware completable future where context changes also propagate across completion stages.
     * @see CompletionStage
     * @see ContextSnapshot#capture()
     */
    public ContextAwareCompletableFuture<T> takeNewSnapshot() {
        return takeNewSnapshot(true);
    }

    /**
     * Returns a context-aware CompletableFuture that may capture a new snapshot after each completion stage.
     *
     * <p>
     * This means that after each {@code then...}, {@code run...}, {@code apply...} method,
     * after calling the function, <strong>a new context snapshot is captured</strong> for follow-up calls.
     *
     * <p>
     * Only set this to {@code true} when chaining completable futures where the completion stages
     * may update contextual values.<br>
     * <strong>Warning:</strong> <em>This may result in unnecessary context snapshots being captured.</em>
     *
     * @param captureNewSnapshot whether new context snapshots must be captured after each completion stage.
     * @return A context-aware completable future where context changes also propagate across completion stages
     * if {@code captureNewSnapshot} is {@code true}.
     * @see CompletionStage
     * @see ContextSnapshot#capture()
     */
    public ContextAwareCompletableFuture<T> takeNewSnapshot(boolean captureNewSnapshot) {
        return this.captureNewSnapshot == captureNewSnapshot ? this : wrap(this, snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U> ContextAwareCompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return wrap(super.thenApply(new FunctionWithContext(snapshotHolder, fn, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U> ContextAwareCompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return wrap(super.thenApplyAsync(new FunctionWithContext(snapshotHolder, fn, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U> ContextAwareCompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return wrap(super.thenApplyAsync(new FunctionWithContext(snapshotHolder, fn, resultSnapshotConsumer()) {
        }, executor), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ContextAwareCompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return wrap(super.thenAccept(new ConsumerWithContext(snapshotHolder, action, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ContextAwareCompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return wrap(super.thenAcceptAsync(new ConsumerWithContext(snapshotHolder, action, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ContextAwareCompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return wrap(super.thenAcceptAsync(new ConsumerWithContext(snapshotHolder, action, resultSnapshotConsumer()) {
        }, executor), snapshotHolder, captureNewSnapshot);
    }

    @Override
    public ContextAwareCompletableFuture<Void> thenRun(Runnable action) {
        return wrap(super.thenRun(new RunnableWithContext(snapshotHolder, action, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return wrap(super.thenRunAsync(new RunnableWithContext(snapshotHolder, action, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    public ContextAwareCompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return wrap(super.thenRunAsync(new RunnableWithContext(snapshotHolder, action, resultSnapshotConsumer()) {
        }, executor), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U, V> ContextAwareCompletableFuture<V> thenCombine(
            CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return wrap(super.thenCombine(other, new BiFunctionWithContext(snapshotHolder, fn, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U, V> ContextAwareCompletableFuture<V> thenCombineAsync(
            CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return wrap(super.thenCombineAsync(other, new BiFunctionWithContext(snapshotHolder, fn, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U, V> ContextAwareCompletableFuture<V> thenCombineAsync(
            CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return wrap(super.thenCombineAsync(other, new BiFunctionWithContext(snapshotHolder, fn, resultSnapshotConsumer()) {
        }, executor), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U> ContextAwareCompletableFuture<Void> thenAcceptBoth(
            CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return wrap(super.thenAcceptBoth(other, new BiConsumerWithContext(snapshotHolder, action, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U> ContextAwareCompletableFuture<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return wrap(super.thenAcceptBothAsync(other, new BiConsumerWithContext(snapshotHolder, action, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U> ContextAwareCompletableFuture<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor) {
        return wrap(super.thenAcceptBothAsync(other, new BiConsumerWithContext(snapshotHolder, action, resultSnapshotConsumer()) {
        }, executor), snapshotHolder, captureNewSnapshot);
    }

    @Override
    public ContextAwareCompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return wrap(super.runAfterBoth(other, new RunnableWithContext(snapshotHolder, action, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    public ContextAwareCompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return wrap(super.runAfterBothAsync(other, new RunnableWithContext(snapshotHolder, action, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    public ContextAwareCompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return wrap(super.runAfterBothAsync(other, new RunnableWithContext(snapshotHolder, action, resultSnapshotConsumer()) {
        }, executor), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U> ContextAwareCompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        // Don't gamble which completion stage might win, create a new holder for the resulting function instead.
        final ContextSnapshotHolder newHolder = new ContextSnapshotHolder(snapshotHolder.get());
        return wrap(super.applyToEither(other, new FunctionWithContext(newHolder, fn, captureNewSnapshot ? newHolder : null) {
        }), newHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U> ContextAwareCompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        // Don't gamble which completion stage might win, create a new holder for the resulting function instead.
        final ContextSnapshotHolder newHolder = new ContextSnapshotHolder(snapshotHolder.get());
        return wrap(super.applyToEitherAsync(other, new FunctionWithContext(newHolder, fn, captureNewSnapshot ? newHolder : null) {
        }), newHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U> ContextAwareCompletableFuture<U> applyToEitherAsync(
            CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor) {
        // Don't gamble which completion stage might win, create a new holder for the resulting function instead.
        final ContextSnapshotHolder newHolder = new ContextSnapshotHolder(snapshotHolder.get());
        return wrap(super.applyToEitherAsync(other, new FunctionWithContext(newHolder, fn, captureNewSnapshot ? newHolder : null) {
        }, executor), newHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ContextAwareCompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        // Don't gamble which completion stage might win, create a new holder for the resulting function instead.
        final ContextSnapshotHolder newHolder = new ContextSnapshotHolder(snapshotHolder.get());
        return wrap(super.acceptEither(other, new ConsumerWithContext(newHolder, action, captureNewSnapshot ? newHolder : null) {
        }), newHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ContextAwareCompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        // Don't gamble which completion stage might win, create a new holder for the resulting function instead.
        final ContextSnapshotHolder newHolder = new ContextSnapshotHolder(snapshotHolder.get());
        return wrap(super.acceptEitherAsync(other, new ConsumerWithContext(newHolder, action, captureNewSnapshot ? newHolder : null) {
        }), newHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ContextAwareCompletableFuture<Void> acceptEitherAsync(
            CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor) {
        // Don't gamble which completion stage might win, create a new holder for the resulting function instead.
        final ContextSnapshotHolder newHolder = new ContextSnapshotHolder(snapshotHolder.get());
        return wrap(super.acceptEitherAsync(other, new ConsumerWithContext(newHolder, action, captureNewSnapshot ? newHolder : null) {
        }, executor), newHolder, captureNewSnapshot);
    }

    @Override
    public ContextAwareCompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        // Don't gamble which completion stage might win, create a new holder for the resulting function instead.
        final ContextSnapshotHolder newHolder = new ContextSnapshotHolder(snapshotHolder.get());
        return wrap(super.runAfterEither(other, new RunnableWithContext(newHolder, action, captureNewSnapshot ? newHolder : null) {
        }), newHolder, captureNewSnapshot);
    }

    @Override
    public ContextAwareCompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        // Don't gamble which completion stage might win, create a new holder for the resulting function instead.
        final ContextSnapshotHolder newHolder = new ContextSnapshotHolder(snapshotHolder.get());
        return wrap(super.runAfterEitherAsync(other, new RunnableWithContext(newHolder, action, captureNewSnapshot ? newHolder : null) {
        }), newHolder, captureNewSnapshot);
    }

    @Override
    public ContextAwareCompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        // Don't gamble which completion stage might win, create a new holder for the resulting function instead.
        final ContextSnapshotHolder newHolder = new ContextSnapshotHolder(snapshotHolder.get());
        return wrap(super.runAfterEitherAsync(other, new RunnableWithContext(newHolder, action, captureNewSnapshot ? newHolder : null) {
        }, executor), newHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U> ContextAwareCompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return wrap(super.thenCompose(new FunctionWithContext(snapshotHolder, fn, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U> ContextAwareCompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return wrap(super.thenComposeAsync(new FunctionWithContext(snapshotHolder, fn, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U> ContextAwareCompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        return wrap(super.thenComposeAsync(new FunctionWithContext(snapshotHolder, fn, resultSnapshotConsumer()) {
        }, executor), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ContextAwareCompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return wrap(super.whenComplete(new BiConsumerWithContext(snapshotHolder, action, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ContextAwareCompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return wrap(super.whenCompleteAsync(new BiConsumerWithContext(snapshotHolder, action, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ContextAwareCompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return wrap(super.whenCompleteAsync(new BiConsumerWithContext(snapshotHolder, action, resultSnapshotConsumer()) {
        }, executor), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U> ContextAwareCompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return wrap(super.handle(new BiFunctionWithContext(snapshotHolder, fn, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U> ContextAwareCompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return wrap(super.handleAsync(new BiFunctionWithContext(snapshotHolder, fn, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <U> ContextAwareCompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return wrap(super.handleAsync(new BiFunctionWithContext(snapshotHolder, fn, resultSnapshotConsumer()) {
        }, executor), snapshotHolder, captureNewSnapshot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ContextAwareCompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return wrap(super.exceptionally(new FunctionWithContext(snapshotHolder, fn, resultSnapshotConsumer()) {
        }), snapshotHolder, captureNewSnapshot);
    }

}
