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
package nl.talsmasoftware.concurrency.futures;

import nl.talsmasoftware.concurrency.context.ContextSnapshot;

import java.util.concurrent.CompletableFuture;

/**
 * This class extends the standard {@link CompletableFuture} that was introduced in java version 8.
 * <p>
 * The class is a 'normal' Completable Future, but every successive call made on the result will be made within the
 * {@link ContextSnapshot context during creation} of this {@link ContextAwareCompletableFuture}.
 *
 * @author Sjoerd Talsma
 * @see nl.talsmasoftware.context.futures.ContextAwareCompletableFuture
 * @deprecated Please switch to <code>nl.talsmasoftware.context.futures.ContextAwareCompletableFuture</code>
 */
public class ContextAwareCompletableFuture<T> extends nl.talsmasoftware.context.futures.ContextAwareCompletableFuture<T> {

}
