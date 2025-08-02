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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class WrapperTest {

    @Test
    void isWrapperOf_null_is_false() {
        Wrapper<Object> subject = new Wrapper<Object>(new Object()) {
        };

        assertThat(subject.isWrapperOf(null), is(false));
    }

    @Test
    void isWrapperOf_other_delegate_is_false() {
        Wrapper<Object> subject = new Wrapper<Object>(new Object()) {
        };

        assertThat(subject.isWrapperOf(new Object()), is(false));
    }

    @Test
    void isWrapperOf_delegate_is_true() {
        Object delegate = new Object();
        Wrapper<Object> subject = new Wrapper<Object>(delegate) {
        };

        assertThat(subject.isWrapperOf(delegate), is(true));
    }
}
