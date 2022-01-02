/*
 * Copyright 2016-2022 Talsma ICT
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
 * Propagate the {@linkplain org.slf4j.MDC SLF4J MDC} content from one thread to another.
 *
 * <p>
 * This context manager maintains no threadlocal state of its own,
 * instead it propagates the
 * existing {@linkplain org.slf4j.MDC#getCopyOfContextMap() MDC context map}
 * into each {@linkplain nl.talsmasoftware.context.ContextSnapshot context snapshot}
 * to be applied again when the snapshot
 * is {@linkplain nl.talsmasoftware.context.ContextSnapshot#reactivate() reactivated}
 * in another thread.
 */
package nl.talsmasoftware.context.slf4j.mdc;