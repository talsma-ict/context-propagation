/*
 * Copyright 2016-2024 Talsma ICT
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
 * Classes adding context support to concurrent usage.
 *
 * <p>
 *
 * <h2>{@linkplain nl.talsmasoftware.context.core.concurrent.ContextAwareExecutorService ContextAwareExecutorService}</h2>
 * <p>
 * An {@code ExecutorService} that propagates a {@linkplain nl.talsmasoftware.context.api.ContextSnapshot ContextSnapshot}
 * to submitted tasks. Any existing {@linkplain java.util.concurrent.ExecutorService ExecutorService} can be used
 * as a delegate, including those from the {@linkplain java.util.concurrent.Executors Executors} utility class.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.core.concurrent.ContextAwareCompletableFuture ContextAwareCompletableFuture}</h2>
 * <p>
 *
 */
package nl.talsmasoftware.context.core.concurrent;
