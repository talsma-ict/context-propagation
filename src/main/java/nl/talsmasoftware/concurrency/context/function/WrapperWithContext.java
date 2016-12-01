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

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Wrapper that also contains a fixed context snapshot.
 *
 * @author Sjoerd Talsma
 */
class WrapperWithContext<T> extends Wrapper<T> {

    protected final ContextSnapshot snapshot;

    protected WrapperWithContext(ContextSnapshot snapshot, T delegate) {
        super(delegate);
        this.snapshot = requireNonNull(snapshot, () -> String.format("No context snapshot provided to %s.", this));
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), snapshot);
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (super.equals(other) && Objects.equals(snapshot, ((WrapperWithContext<?>) other).snapshot));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{delegate=" + Objects.toString(delegate(), "<null>")
                + ", snapshot=" + snapshot + '}';
    }

}
