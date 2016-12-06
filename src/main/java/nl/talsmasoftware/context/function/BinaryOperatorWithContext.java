/*
 * Copyright (C) 2016 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.talsmasoftware.context.function;

import nl.talsmasoftware.context.ContextSnapshot;

import java.util.function.BinaryOperator;

/**
 * A wrapper for {@link BinaryOperator} that {@link ContextSnapshot#reactivate() reactivates a context snapshot} before
 * calling a delegate.
 *
 * @author Sjoerd Talsma
 */
public class BinaryOperatorWithContext<T> extends BiFunctionWithContext<T, T, T> implements BinaryOperator<T> {

    public BinaryOperatorWithContext(ContextSnapshot snapshot, BinaryOperator<T> delegate) {
        super(snapshot, delegate);
    }

}