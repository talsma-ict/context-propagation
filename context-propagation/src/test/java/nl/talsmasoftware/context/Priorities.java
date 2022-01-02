/*
 * Copyright 2016-2022 Talsma ICT
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
package nl.talsmasoftware.context;

import javax.annotation.Priority;

final class Priorities {
    static final Object minus5 = new Minus5();
    static final Object minus3 = new Minus3();
    static final Object zero = new Zero();
    static final Object one = new One();
    static final Object inheritedOne = new InheritedOne();
    static final Object two = new Two();
    static final Object noPriority = new NoPriority();

    @Priority(-5)
    static class Minus5 {
    }

    @Priority(-3)
    static class Minus3 {
    }

    @Priority(0)
    static class Zero {
    }

    @Priority(1)
    static class One {
    }

    static class InheritedOne extends One {
    }

    @Priority(2)
    static class Two {
    }

    static class NoPriority {
    }

}