/*
 * Copyright 2016-2025 Talsma ICT
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
package nl.talsmasoftware.context.delegation;

import nl.talsmasoftware.context.ContextSnapshot;

/**
 * Interface for a {@code Supplier} of {@link ContextSnapshot}.
 * <p>
 * Merely an equivalent of the functional interface
 * {@code java.util.Supplier<ContextSnapshot>}
 * in order to retain JDK 5 compatibility.
 *
 * @author Sjoerd Talsma
 * @deprecated This will be replace by {@code Supplier<ContextSnapshot>} from Java 8.
 */
@Deprecated
public interface ContextSnapshotSupplier {

    /**
     * Supply a {@linkplain ContextSnapshot snapshot}.
     * <p>
     * The supplier interface itself makes no guarantees whether the snapshot is nullable or not.
     *
     * @return The context snapshot.
     */
    ContextSnapshot get();

}
