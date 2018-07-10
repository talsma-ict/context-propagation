/*
 * Copyright 2016-2018 Talsma ICT
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
import nl.talsmasoftware.context.delegation.WrapperWithContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Package-convenience subclass for {@linkplain WrapperWithContext} that takes Java 8 generic functional interfaces
 * {@link Supplier} and {@link Consumer} instead of the specific Java 5 versions.
 *
 * @param <T> The type of the wrapped delegate object.
 */
abstract class Java8WrapperWithContext<T> extends WrapperWithContext<T> {

    protected Java8WrapperWithContext(Supplier<ContextSnapshot> supplier, T delegate, Consumer<ContextSnapshot> consumer) {
        super(
                requireNonNull(supplier, "Context snapshot supplier is <null>.")::get,
                delegate,
                consumer == null ? null : consumer::accept
        );
    }

}
