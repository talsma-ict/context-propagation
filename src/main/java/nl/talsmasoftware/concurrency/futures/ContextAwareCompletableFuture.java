/*
 * Copyright (C) 2016 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.talsmasoftware.concurrency.futures;

import nl.talsmasoftware.concurrency.context.ContextManagers;
import nl.talsmasoftware.concurrency.context.ContextSnapshot;
import nl.talsmasoftware.concurrency.context.function.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class extends the standard {@link CompletableFuture} that was introduced in java version 8.
 * <p>
 * The class is a 'normal' Completable Future, but every successive call made on the result will be made within the
 * {@link ContextSnapshot context during creation} of this {@link ContextAwareCompletableFuture}.
 *
 * @author Sjoerd Talsma
 */
public class ContextAwareCompletableFuture<T> extends CompletableFuture<T> {

    /**
     * A snapshot of the context as it was when this <code>CompletableFuture</code> was created.
     */
    private final ContextSnapshot snapshot = ContextManagers.createContextSnapshot();

    @Override
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return super.thenApply(new FunctionWithContext<>(snapshot, fn));
    }

    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return super.thenApplyAsync(new FunctionWithContext<>(snapshot, fn));
    }

    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return super.thenApplyAsync(new FunctionWithContext<>(snapshot, fn), executor);
    }

    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return super.thenAccept(new ConsumerWithContext<>(snapshot, action));
    }

    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return super.thenAcceptAsync(new ConsumerWithContext<>(snapshot, action));
    }

    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return super.thenAcceptAsync(new ConsumerWithContext<>(snapshot, action), executor);
    }

    public CompletableFuture<Void> thenRun(Runnable action) {
        return super.thenRun(new RunnableWithContext(snapshot, action));
    }

    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return super.thenRunAsync(new RunnableWithContext(snapshot, action));
    }

    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return super.thenRunAsync(new RunnableWithContext(snapshot, action), executor);
    }

    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return super.thenCombine(other, new BiFunctionWithContext<>(snapshot, fn));
    }

    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return super.thenCombineAsync(other, new BiFunctionWithContext<>(snapshot, fn));
    }

    public <U, V> CompletableFuture<V> thenCombineAsync(
            CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return super.thenCombineAsync(other, new BiFunctionWithContext<>(snapshot, fn), executor);
    }

    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return super.thenAcceptBoth(other, new BiConsumerWithContext<>(snapshot, action));
    }

    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return super.thenAcceptBothAsync(other, new BiConsumerWithContext<>(snapshot, action));
    }

    public <U> CompletableFuture<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor) {
        return super.thenAcceptBothAsync(other, new BiConsumerWithContext<>(snapshot, action), executor);
    }

    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return super.runAfterBoth(other, new RunnableWithContext(snapshot, action));
    }

    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return super.runAfterBothAsync(other, new RunnableWithContext(snapshot, action));
    }

    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return super.runAfterBothAsync(other, new RunnableWithContext(snapshot, action), executor);
    }

    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return super.applyToEither(other, new FunctionWithContext<>(snapshot, fn));
    }

    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return super.applyToEitherAsync(other, new FunctionWithContext<>(snapshot, fn));
    }

    public <U> CompletableFuture<U> applyToEitherAsync(
            CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor) {
        return super.applyToEitherAsync(other, new FunctionWithContext<>(snapshot, fn), executor);
    }

    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return super.acceptEither(other, new ConsumerWithContext<>(snapshot, action));
    }

    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return super.acceptEitherAsync(other, new ConsumerWithContext<>(snapshot, action));
    }

    public CompletableFuture<Void> acceptEitherAsync(
            CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor) {
        return super.acceptEitherAsync(other, new ConsumerWithContext<>(snapshot, action), executor);
    }

    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return super.runAfterEither(other, new RunnableWithContext(snapshot, action));
    }

    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return super.runAfterEitherAsync(other, new RunnableWithContext(snapshot, action));
    }

    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return super.runAfterEitherAsync(other, new RunnableWithContext(snapshot, action), executor);
    }

    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return super.thenCompose(new FunctionWithContext<>(snapshot, fn));
    }

    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return super.thenComposeAsync(new FunctionWithContext<>(snapshot, fn));
    }

    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        return super.thenComposeAsync(new FunctionWithContext<>(snapshot, fn), executor);
    }

    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return super.whenComplete(new BiConsumerWithContext<>(snapshot, action));
    }

    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return super.whenCompleteAsync(new BiConsumerWithContext<>(snapshot, action));
    }

    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return super.whenCompleteAsync(new BiConsumerWithContext<>(snapshot, action), executor);
    }

    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return super.handle(new BiFunctionWithContext<>(snapshot, fn));
    }

    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return super.handleAsync(new BiFunctionWithContext<>(snapshot, fn));
    }

    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return super.handleAsync(new BiFunctionWithContext<>(snapshot, fn), executor);
    }

    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return super.exceptionally(new FunctionWithContext<>(snapshot, fn));
    }

}
