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

import nl.talsmasoftware.context.api.ContextSnapshot;
import nl.talsmasoftware.context.api.ContextSnapshot.Reactivation;
import nl.talsmasoftware.context.managers.locale.CurrentLocaleHolder;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class JigsawLocalePropagator {

    public static Locale getPropagatedLocaleFromNewThread() throws InterruptedException {
        final ContextSnapshot snapshot = ContextSnapshot.capture();
        final AtomicReference<Locale> result = new AtomicReference<>();
        final Thread thread = new Thread(() -> {
            try (Reactivation reactivation = snapshot.reactivate()) {
                result.set(CurrentLocaleHolder.get().orElse(null));
            }
        });
        thread.start();
        thread.join(10000L);
        return result.get();
    }

}
