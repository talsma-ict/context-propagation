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
package nl.talsmasoftware.context.core.function;

import nl.talsmasoftware.context.api.ContextSnapshot;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A wrapper for {@link UnaryOperator} that {@link ContextSnapshot#reactivate() reactivates a context snapshot} before
 * calling a delegate.
 *
 * @author Sjoerd Talsma
 */
public class UnaryOperatorWithContext<T> extends FunctionWithContext<T, T> implements UnaryOperator<T> {

    public UnaryOperatorWithContext(ContextSnapshot snapshot, UnaryOperator<T> delegate) {
        this(snapshot, delegate, null);
    }

    public UnaryOperatorWithContext(ContextSnapshot snapshot, UnaryOperator<T> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(snapshot, delegate, snapshotConsumer);
    }

    protected UnaryOperatorWithContext(Supplier<ContextSnapshot> supplier, UnaryOperator<T> delegate, Consumer<ContextSnapshot> snapshotConsumer) {
        super(supplier, delegate, snapshotConsumer);
    }
}
