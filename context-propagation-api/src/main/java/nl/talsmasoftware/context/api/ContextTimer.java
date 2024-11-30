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
package nl.talsmasoftware.context.api;

import java.util.concurrent.TimeUnit;

/**
 * Minimal Service Provider Interface for services that wish to get informed of context switching metrics.
 * <p>
 * Currently, the following timed operations are updated:
 * <ul>
 * <li>{@linkplain ContextSnapshot#capture()}</li>
 * <li>{@linkplain ContextSnapshot#reactivate()}</li>
 * <li>{@linkplain ContextManager#getActiveContextValue()} (*)</li>
 * <li>{@linkplain ContextManager#initializeNewContext(Object)} (*)</li>
 * </ul>
 * <p>
 * (*) <em>Timing is updated for each concrete {@code ContextManager} implementation class</em>
 *
 * @author Sjoerd Talsma
 * @since 2.0.0
 */
public interface ContextTimer {

    /**
     * Provides a new update for the context timer.
     *
     * @param type     Class that was called
     * @param method   Method that was called
     * @param duration Duration of the call
     * @param unit     Unit of the duration
     * @param error    Error that was thrown in the call (optional, normally {@code null})
     */
    void update(Class<?> type, String method, long duration, TimeUnit unit, Throwable error);

}
