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
package nl.talsmasoftware.context.managers.locale;

import nl.talsmasoftware.context.api.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CurrentLocaleHolderTest {
    @BeforeEach
    @AfterEach
    void clearCurrentLocaleHolder() {
        CurrentLocaleHolder.clear();
    }

    @Test
    void verifyPropagationIntoNewThread() throws InterruptedException {
        // prepare
        final Locale[] result = new Locale[1];
        CurrentLocaleHolder.set(Locale.GERMAN);
        Thread thread = new Thread(() -> result[0] = CurrentLocaleHolder.getOrDefault());

        // execute
        thread.start();
        thread.join(10000L);

        // verify
        assertThat(result[0]).isEqualTo(Locale.GERMAN);
    }

    @Test
    void outOfSequenceClosingMustBeSupported() {
        // ctx1 = GERMAN
        Context<Locale> ctx1 = CurrentLocaleHolder.set(Locale.GERMAN);
        assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(Locale.GERMAN);

        // ctx2 = ENGLISH
        Context<Locale> ctx2 = CurrentLocaleHolder.set(Locale.ENGLISH);
        assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(Locale.ENGLISH);

        // out-of-sequence, ctx1 is closed first.
        ctx1.close();
        assertThat(CurrentLocaleHolder.getOrDefault()).isEqualTo(Locale.ENGLISH);

        // when ctx2 is closed, because ctx1 was already closed, the remaining context is empty
        ctx2.close();
        assertThat(CurrentLocaleHolder.get()).isEmpty();
    }

    @Test
    void testToString_current() {
        Context<Locale> currentContext = CurrentLocaleHolder.set(Locale.ENGLISH);
        assertThat(currentContext).hasToString("CurrentLocaleHolder{en}");
    }

    @Test
    void testToString_closed() {
        Context<Locale> closedContext = CurrentLocaleHolder.set(Locale.ENGLISH);
        closedContext.close();

        assertThat(closedContext).hasToString("CurrentLocaleHolder{closed}");
    }

    @Test
    void testClose_mustBeRepeatable() {
        Context<Locale> context = CurrentLocaleHolder.set(Locale.ENGLISH);
        assertDoesNotThrow(context::close);
        assertDoesNotThrow(context::close);
        assertDoesNotThrow(context::close);
        assertDoesNotThrow(context::close);
    }
}
