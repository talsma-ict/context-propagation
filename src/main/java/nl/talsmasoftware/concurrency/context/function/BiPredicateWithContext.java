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

package nl.talsmasoftware.concurrency.context.function;

import nl.talsmasoftware.concurrency.context.ContextSnapshot;

import java.util.function.BiPredicate;

/**
 * A wrapper for {@link BiPredicate} that {@link ContextSnapshot#reactivate() reactivates a context snapshot} before
 * calling a delegate.
 *
 * @author Sjoerd Talsma
 * @see nl.talsmasoftware.context.functions.BiPredicateWithContext
 * @deprecated Please switch to <code>nl.talsmasoftware.context.functions.BiPredicateWithContext</code>
 */
public class BiPredicateWithContext<IN1, IN2> extends nl.talsmasoftware.context.functions.BiPredicateWithContext<IN1, IN2> {

    public BiPredicateWithContext(ContextSnapshot snapshot, BiPredicate<IN1, IN2> delegate) {
        super(snapshot, delegate);
    }

}
