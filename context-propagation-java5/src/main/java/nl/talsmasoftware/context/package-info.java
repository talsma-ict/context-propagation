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

/**
 * The core concepts of the context-propagation library.
 * <br>
 * <dl>
 * <dt><a href="Context.html">Context</a>:</dt>
 * <dd>Defines a context as something with a value that can be closed.</dd>
 * <dt><a href="ContextManager.html">ContextManager</a>:</dt>
 * <dd>Interface defining required operations to manage contexts:
 * creating new contexts and obtaining 'current' contexts.</dd>
 * <dt><a href="ContextSnapshot.html">ContextSnapshot</a>:</dt>
 * <dd>A snapshot of one or more active contexts
 * that can be reactivated temporarily (until closed).</dd>
 * <dt><a href="ContextManagers.html">ContextManagers</a>:</dt>
 * <dd>Contains a static method to create a new snapshot
 * from registered ContextManager implementations.</dd>
 * </dl>
 *
 * @author Sjoerd Talsma
 */
package nl.talsmasoftware.context;