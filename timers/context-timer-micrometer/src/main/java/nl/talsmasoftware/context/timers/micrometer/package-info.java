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
/**
 * {@link nl.talsmasoftware.context.api.ContextTimer Context timer} that
 * updates a {@linkplain io.micrometer.core.instrument.Timer micrometer Timer}
 * to {@linkplain io.micrometer.core.instrument.Timer#record(long, TimeUnit) record}
 * the context switching durations.
 */
package nl.talsmasoftware.context.timers.micrometer;

import java.util.concurrent.TimeUnit;
