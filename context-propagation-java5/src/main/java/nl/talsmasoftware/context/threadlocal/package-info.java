/*
 * Copyright 2016-2019 Talsma ICT
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
 * Package for {@code ThreadLocal} specific classes.
 *
 * <h2>{@linkplain nl.talsmasoftware.context.threadlocal.AbstractThreadLocalContext}</h2>
 * <p>
 * Base class for all our own context implementations.<br>
 * It features:
 * <ul>
 * <li>Nesting; the previously active context is remembered when activating a new one,
 * so it can be restored after the new context ends.
 * <li>Predictable behaviour when used in a wrong way. Out-of-order closing can happen and will
 * result in 'skipping' an already closed context as the currently active context.
 * Closing from a different thread than where the context was created is wrong, however the
 * AbstractThreadLocalContext will deal with it.
 * </ul>
 */
package nl.talsmasoftware.context.threadlocal;