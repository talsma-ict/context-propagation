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
package nl.talsmasoftware.context.timing;

/**
 * Minimal Service Provider Interface for services that wish to get informed of context switching metrics.
 * <p>
 * Currently the following timed operations are updated:
 * <ul>
 * <li>{@code ContextManagers.createContextSnapshot}</li>
 * <li>{@code ContextSnapshot.reactivate}</li>
 * <li>{@code ContextManager.initializeNewContext}(*)</li>
 * <li>{@code ContextManager.getActiveContext}(*)</li>
 * </ul>
 * <p>
 * (*) <em>Timing is updated for each concrete {@code ContextManager} implementation class</em>
 *
 * @author Sjoerd Talsma
 * @deprecated Moved to package {@code nl.talsmasoftware.context.api}
 */
@Deprecated
public interface ContextTimer extends nl.talsmasoftware.context.api.ContextTimer {

}
