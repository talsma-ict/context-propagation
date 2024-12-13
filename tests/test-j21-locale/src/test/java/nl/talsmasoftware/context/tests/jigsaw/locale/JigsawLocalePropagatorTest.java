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
package nl.talsmasoftware.context.tests.jigsaw.locale;

import nl.talsmasoftware.context.managers.locale.CurrentLocaleHolder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class JigsawLocalePropagatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"nl-NL", "en-GB", "de-DE"})
    void testLocalePropagation(String locale) throws InterruptedException {
        // given
        final Locale currentLocale = parseLocale(locale);
        CurrentLocaleHolder.set(currentLocale);

        // when
        final Locale result = JigsawLocalePropagator.getPropagatedLocaleFromNewThread();

        // then
        assertThat(result).isEqualTo(currentLocale);
    }

    private static Locale parseLocale(String locale) {
        return Locale.forLanguageTag(locale.replace("_", "-"));
    }
}
