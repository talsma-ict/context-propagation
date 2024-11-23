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
 * Manager and holder for a current {@linkplain java.util.Locale Locale}.
 *
 * <p>
 * Setting a current locale can be done using {@linkplain nl.talsmasoftware.context.managers.locale.CurrentLocaleHolder#set(java.util.Locale)}
 * <pre>{@code
 * private void runWithLocale(Locale locale, Runnable someCode) {
 *     try (Context<Locale> ctx = CurrentLocaleHolder.set(locale)) {
 *         someCode.run();
 *     }
 * }
 * }</pre>
 *
 * <p>
 * Obtaining the current locale works similar:
 * <pre>{@code
 * private void someCode() {
 *     Optional<Locale> currentLocale = CurrentLocaleHolder.get();
 *     Locale currentOrDefaultLocale = CurrentLocaleHolder.getOrDefault(); // short for get().orElseGet(Locale::getDefault)
 *     // ...
 * }
 * }</pre>
 *
 * <p>
 * The supplied {@linkplain nl.talsmasoftware.context.managers.locale.CurrentLocaleManager CurrentLocaleManager} will
 * make sure the current locale is propagated into threads that support the
 * {@linkplain nl.talsmasoftware.context.api.ContextManager ContextManager} framework,
 * such as code using {@code ContextAwareExecutorService} or {@code ContextAwareCompletableFuture}.
 */
package nl.talsmasoftware.context.managers.locale;
