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
package nl.talsmasoftware.context.functions;

import nl.talsmasoftware.context.ContextSnapshot;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A wrapper for {@link BiFunction} that {@link ContextSnapshot#reactivate() reactivates a context snapshot} before
 * calling a delegate.
 *
 * @author Sjoerd Talsma
 * @deprecated Moved to package {@code nl.talsmasoftware.context.core.function}.
 */
@Deprecated
public class BiFunctionWithContext<IN1, IN2, OUT> extends nl.talsmasoftware.context.core.function.BiFunctionWithContext<IN1, IN2, OUT> {

    public BiFunctionWithContext(ContextSnapshot snapshot, BiFunction<IN1, IN2, OUT> delegate) {
        super(snapshot, delegate);
    }

    public BiFunctionWithContext(ContextSnapshot snapshot, BiFunction<IN1, IN2, OUT> delegate, Consumer<ContextSnapshot> consumer) {
        super(snapshot, delegate, consumer);
    }

    protected BiFunctionWithContext(Supplier<ContextSnapshot> supplier, BiFunction<IN1, IN2, OUT> delegate, Consumer<ContextSnapshot> consumer) {
        super(supplier, delegate, consumer);
    }

}
