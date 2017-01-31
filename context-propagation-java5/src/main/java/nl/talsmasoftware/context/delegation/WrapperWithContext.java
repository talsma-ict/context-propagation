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
package nl.talsmasoftware.context.delegation;

import nl.talsmasoftware.context.ContextSnapshot;

/**
 * Wrapper that also contains a fixed context snapshot.
 *
 * @author Sjoerd Talsma
 */
public abstract class WrapperWithContext<T> extends Wrapper<T> {

    protected final ContextSnapshot snapshot;

    protected WrapperWithContext(ContextSnapshot snapshot, T delegate) {
        super(delegate);
        this.snapshot = snapshot;
        if (snapshot == null) {
            throw new NullPointerException(String.format("No context snapshot provided to %s.", this));
        }
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + snapshot.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (super.equals(other) && snapshot.equals(((WrapperWithContext<?>) other).snapshot));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{delegate=" + delegate() + ", snapshot=" + snapshot + '}';
    }

}
