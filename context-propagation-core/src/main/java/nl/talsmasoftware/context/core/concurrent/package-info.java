/*
 * Copyright 2016-2025 Talsma ICT
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
/**
 * Context management for ThreadLocal values in concurrent applications.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.core.concurrent.ContextAwareExecutorService ContextAwareExecutorService}</h2>
 * <p>
 * Executor service that wraps another {@linkplain java.util.concurrent.ExecutorService ExecutorService},
 * making sure background tasks operate 'within'
 * a {@linkplain nl.talsmasoftware.context.api.ContextSnapshot context snapshot} taken from the submitting thread.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.core.concurrent.ContextAwareCompletableFuture ContextAwareCompletableFuture}</h2>
 * <p>
 * {@linkplain java.util.concurrent.CompletableFuture CompletableFuture} that runs every successive call with a reactivated
 * {@linkplain nl.talsmasoftware.context.api.ContextSnapshot context snapshot} taken from the submitting thread.
 */
package nl.talsmasoftware.context.core.concurrent;
