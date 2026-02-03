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
 * Manager to propagate the Slf4J {@linkplain org.slf4j.MDC MDC context map} from one thread to another.
 *
 * <p>
 * This context manager maintains no ThreadLocal state of its own,
 * instead it propagates the current {@linkplain org.slf4j.MDC MDC} values
 * into each {@linkplain nl.talsmasoftware.context.api.ContextSnapshot context snapshot} that is captured.
 *
 * <p>
 * Upon {@linkplain nl.talsmasoftware.context.api.ContextSnapshot#reactivate() reactivation},
 * these captured values (and <em>only</em> these captured values) are reactivated in the MDC.
 * <ul>
 *  <li>MDC keys that exist in the target MDC which are <em>not</em> part of the snapshot are left unchanged.
 *  <li>MDC keys with the case-insensitive substring {@code "thread"}, are <em>not</em> captured
 *      in the {@linkplain nl.talsmasoftware.context.api.ContextSnapshot},
 *      since they most-likely contain a thread-specific value.
 *  <li>When a reactivation is <em>closed</em>, the <em>previous</em> MDC values <em>for the captured keys</em> are restored.
 *      All other keys that are not part of the context snapshot will be left unchanged.
 * </ul>
 */
package nl.talsmasoftware.context.managers.slf4j.mdc;
