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
 * Maintain a current {@linkplain java.util.Locale Locale} context.
 *
 * <p>
 * Setting a current locale can be done using the {@linkplain nl.talsmasoftware.context.managers.locale.LocaleContextManager}:
 * <pre>{@code
 * private static LocaleContextManager localeContextManager = new LocaleContextManager();
 *
 * private void runWithLocale(Locale locale, Runnable someCode) {
 *     try (Context<Locale> ctx = localeContextManager.initializeNewContext(locale)) {
 *         someCode.run();
 *     }
 * }
 * }</pre>
 *
 * <p>
 * Obtaining the current locale works similar:
 * <pre>{@code
 * private void someCode() {
 *     Locale currentLocale = LocaleContextManager.getCurrentLocaleOrDefault();
 *     // ...
 * }
 * }</pre>
 */
package nl.talsmasoftware.context.managers.locale;
