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
 * Package for context aware executors.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.executors.ContextAwareExecutorService}</h2>
 * <p>
 * Executor service that wraps another {@linkplain java.util.concurrent.ExecutorService},
 * making sure the background task operates 'within' a context snapshot taken from the submitting thread.
 *
 * <p>
 * The executor service will make sure to close the reactivated snapshot again after the code in the task is finished,
 * even if it throws an exception.
 *
 * <p>
 * Both {@link java.util.concurrent.Callable} and {@link java.lang.Runnable} tasks are mapped.
 */
package nl.talsmasoftware.context.executors;