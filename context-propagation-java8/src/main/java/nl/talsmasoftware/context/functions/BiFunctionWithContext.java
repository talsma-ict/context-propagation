/**
 * Copyright 2016-2017 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package nl.talsmasoftware.context.functions;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.delegation.WrapperWithContext;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * A wrapper for {@link BiFunction} that {@link ContextSnapshot#reactivate() reactivates a context snapshot} before
 * calling a delegate.
 *
 * @author Sjoerd Talsma
 */
public class BiFunctionWithContext<IN1, IN2, OUT> extends WrapperWithContext<BiFunction<IN1, IN2, OUT>> implements BiFunction<IN1, IN2, OUT> {
    private static final Logger LOGGER = Logger.getLogger(BiFunctionWithContext.class.getName());

    public BiFunctionWithContext(ContextSnapshot snapshot, BiFunction<IN1, IN2, OUT> delegate) {
        super(snapshot, delegate);
    }

    @Override
    public OUT apply(IN1 in1, IN2 in2) {
        try (Context<Void> context = snapshot.reactivate()) {
            LOGGER.log(Level.FINEST, "Delegating apply method with {0} to {1}.", new Object[]{context, delegate()});
            return nonNullDelegate().apply(in1, in2);
        }
    }

    @Override
    public <V> BiFunction<IN1, IN2, V> andThen(Function<? super OUT, ? extends V> after) {
        requireNonNull(after, "Cannot post-process bi-function with after function <null>.");
        return (IN1 in1, IN2 in2) -> {
            try (Context<Void> context = snapshot.reactivate()) {
                LOGGER.log(Level.FINEST, "Delegating andThen method with {0} to {1}.", new Object[]{context, delegate()});
                return after.apply(nonNullDelegate().apply(in1, in2));
            }
        };
    }

}
