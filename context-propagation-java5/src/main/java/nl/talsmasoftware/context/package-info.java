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
 * Main package defining the core {@code context-propagation} concepts in this library
 *
 * <h2><a href="Context.html">Context</a></h2>
 * <p>
 * Defines a context as something with a value that can be closed.
 *
 * <h2><a href="ContextManager.html">ContextManager</a></h2>
 * <p>
 * Interface defining required operations to manage contexts:
 * creating new contexts and obtaining 'current' contexts.
 *
 * <h2><a href="ContextSnapshot.html">ContextSnapshot</a></h2>
 * <p>
 * A snapshot of one or more active contexts
 * that can be reactivated temporarily (until closed).
 *
 * <h2><a href="ContextManagers.html">ContextManagers</a></h2>
 * <p>
 * Contains a static method to create a new snapshot
 * from registered ContextManager implementations.
 *
 * @author Sjoerd Talsma
 */
package nl.talsmasoftware.context;