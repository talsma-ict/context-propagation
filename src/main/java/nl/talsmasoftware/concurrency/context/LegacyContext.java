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
package nl.talsmasoftware.concurrency.context;

import static java.util.Objects.requireNonNull;

/**
 * @author Sjoerd Talsma
 * @deprecated This only exists to allows old ContextManagers to delegate to new implementations.
 */
public class LegacyContext<T> implements Context<T>, nl.talsmasoftware.context.Context<T> {

    private final nl.talsmasoftware.context.Context<T> wrapped;

    protected LegacyContext(nl.talsmasoftware.context.Context<T> wrapped) {
        this.wrapped = requireNonNull(wrapped, "Wrapped context is <null>.");
    }

    public static <T> Context<T> wrap(nl.talsmasoftware.context.Context<T> ctx) {
        return ctx == null || ctx instanceof Context ? (Context<T>) ctx : new LegacyContext<>(ctx);
    }

    @Override
    public T getValue() {
        return wrapped.getValue();
    }

    @Override
    public void close() {
        wrapped.close();
    }

    @Override
    public String toString() {
        return "LegacyContext{" + wrapped + '}';
    }

}
