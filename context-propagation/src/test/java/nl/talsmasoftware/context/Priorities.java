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
package nl.talsmasoftware.context;

import javax.annotation.Priority;

public final class Priorities {
    public static final Object minus5 = new Minus5();
    public static final Object minus3 = new Minus3();
    public static final Object zero = new Zero();
    public static final Object one = new One();
    public static final Object inheritedOne = new InheritedOne();
    public static final Object two = new Two();
    public static final Object noPriority = new NoPriority();

    @Priority(-5)
    public static class Minus5 {
    }

    @Priority(-3)
    public static class Minus3 {
    }

    @Priority(0)
    public static class Zero {
    }

    @Priority(1)
    public static class One {
    }

    public static class InheritedOne extends One {
    }

    @Priority(2)
    public static class Two {
    }

    public static class NoPriority {
    }

}
