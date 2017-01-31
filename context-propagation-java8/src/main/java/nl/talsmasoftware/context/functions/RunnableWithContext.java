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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper for {@link Runnable} that {@link ContextSnapshot#reactivate() reactivates a context snapshot} before
 * calling a delegate.
 *
 * @author Sjoerd Talsma
 */
public class RunnableWithContext extends WrapperWithContext<Runnable> implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(RunnableWithContext.class.getName());

    public RunnableWithContext(ContextSnapshot snapshot, Runnable delegate) {
        super(snapshot, delegate);
    }

    @Override
    public void run() {
        try (Context<Void> context = snapshot.reactivate()) {
            LOGGER.log(Level.FINEST, "Delegating run method with {0} to {1}.", new Object[]{context, delegate()});
            nonNullDelegate().run();
        }
    }

}
