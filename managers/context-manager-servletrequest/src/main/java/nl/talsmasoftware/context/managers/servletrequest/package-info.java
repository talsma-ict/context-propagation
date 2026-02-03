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
/**
 * Manager to propagate the current {@linkplain javax.servlet.ServletRequest ServletRequest} from one thread to another.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.managers.servletrequest.ServletRequestContextFilter ServletRequestContextFilter}</h2>
 * <p>
 * This {@linkplain javax.servlet.Filter servlet Filter} activates each servlet request as
 * a {@code ServletRequestContext} on the current thread and makes sure to close it again after the request is handled.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.managers.servletrequest.ServletRequestContextManager ServletRequestContextManager}</h2>
 * <p>
 * Adding the {@code context-manager-servletrequest} library to the classpath will automatically
 * include the {@linkplain nl.talsmasoftware.context.managers.servletrequest.ServletRequestContext active ServletRequest}
 * in {@linkplain nl.talsmasoftware.context.api.ContextSnapshot ContextSnapshots}.
 * This propagates the context to other threads using the
 * {@code ContextAwareExecutorService} or {@code ContextAwareCompletableFuture}.
 *
 * <p>
 * Also, any function <em>..WithContext</em> in the {@code nl.talsmasoftware.context.core.function} package
 * automatically activates the context snapshot around the function body.
 */
package nl.talsmasoftware.context.managers.servletrequest;
