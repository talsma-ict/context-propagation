/*
 * Copyright (C) 2016 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/**
 * This package contains the core concepts of this context-propagation library.
 * <br>
 * <ul>
 * <li><a href="Context.html">Context</a>: Interface defining a context something with a value that can be closed.</li>
 * <li><a href="ContextManager.html">ContextManager</a>: Interface defining required operations to manage contexts: creating new contexts and obtaining 'current' contexts.</li>
 * <li><a href="ContextSnapshot.html">ContextSnapshot</a>: Interface for a snapshot of one or more contexts that can be reactivated temporarily (until closed).</li>
 * <li><a href="ContextManagers.html">ContextManagers</a>: ContextManagers utility class to create a new snapshot
 * of all known active Contexts from registered ContextManager implementations.</li>
 * </ul>
 * <p>
 * The relation between these concepts are represented in the following class diagram for this package:<br>
 * <center><img src="package.svg" alt="Package classes"></center>
 *
 * @author Sjoerd Talsma
 */
package nl.talsmasoftware.context;