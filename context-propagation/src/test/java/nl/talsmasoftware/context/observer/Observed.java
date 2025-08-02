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
package nl.talsmasoftware.context.observer;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.Arrays;

public final class Observed {

    public enum Action {
        ACTIVATE, DEACTIVATE
    }

    final Action action;
    final Object value;

    Observed(Action action, Object value, Object previous) {
        this.action = action;
        this.value = value;
    }

    public static Matcher<Observed> activated(final Matcher<?> delegate) {
        return new BaseMatcher<Observed>() {
            public boolean matches(Object actual) {
                return actual instanceof Observed
                        && Action.ACTIVATE.equals(((Observed) actual).action)
                        && delegate.matches(((Observed) actual).value);
            }

            public void describeTo(Description description) {
                delegate.describeTo(description.appendText("<Activated> "));
            }
        };
    }

    public static Matcher<Observed> deactivated(final Matcher<?> delegate) {
        return new BaseMatcher<Observed>() {
            public boolean matches(Object actual) {
                return actual instanceof Observed
                        && Action.DEACTIVATE.equals(((Observed) actual).action)
                        && delegate.matches(((Observed) actual).value);
            }

            public void describeTo(Description description) {
                delegate.describeTo(description.appendText("<Deactivated> "));
            }
        };
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{action, value});
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Observed
                && equals(this.action, ((Observed) other).action)
                && equals(this.value, ((Observed) other).value)
        );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{action=" + action + ", value=" + value + '}';
    }

    private static boolean equals(Object obj1, Object obj2) {
        return obj1 == null ? obj2 == null : obj1.equals(obj2);
    }

}
