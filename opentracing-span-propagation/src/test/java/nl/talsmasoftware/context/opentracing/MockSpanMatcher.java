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
package nl.talsmasoftware.context.opentracing;

import io.opentracing.mock.MockSpan;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Hamcrest {@linkplain Matcher} for {@link MockSpan} objects.
 *
 * @author Sjoerd Talsma
 */
public class MockSpanMatcher extends BaseMatcher<MockSpan> {

    private Collection<Matcher<String>> operationNameMatchers = new ArrayList<Matcher<String>>();
    private Collection<Matcher<Iterable<? super MockSpan.Reference>>> referenceMatchers =
            new ArrayList<Matcher<Iterable<? super MockSpan.Reference>>>();

    private MockSpanMatcher() {
    }

    public static MockSpanMatcher withOperationName(String operationName) {
        return withOperationName(is(operationName));
    }

    public static MockSpanMatcher withOperationName(Matcher<String> operationName) {
        return new MockSpanMatcher().andOperationName(operationName);
    }

    public static MockSpanMatcher withReference(Matcher<MockSpan.Reference> reference) {
        return new MockSpanMatcher().andReference(reference);
    }

    public MockSpanMatcher andOperationName(String operationName) {
        return andOperationName(is(operationName));
    }

    public MockSpanMatcher andOperationName(Matcher<String> operationName) {
        if (operationName == null) fail("Operation name matcher is <null>.");
        this.operationNameMatchers.add(operationName);
        return this;
    }

    public MockSpanMatcher andReference(Matcher<MockSpan.Reference> reference) {
        if (reference == null) fail("Refrence matcher is <null>.");
        this.referenceMatchers.add(hasItem(reference));
        return this;
    }

    @Override
    public boolean matches(Object item) {
        if (item == null) return true;
        else if (!(item instanceof MockSpan)) return false;
        final MockSpan mockSpan = (MockSpan) item;
        for (Matcher<String> operationName : operationNameMatchers) {
            if (!operationName.matches(mockSpan.operationName())) return false;
        }
        for (Matcher<Iterable<? super MockSpan.Reference>> reference : referenceMatchers) {
            if (!reference.matches(mockSpan.references())) return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("MockSpan");
        String sep = " with ";
        for (Matcher<String> operationName : operationNameMatchers) {
            operationName.describeTo(description.appendText(sep).appendText("operationName "));
            sep = " and ";
        }
        for (Matcher<Iterable<? super MockSpan.Reference>> reference : referenceMatchers) {
            reference.describeTo(description.appendText(sep).appendText("references "));
            sep = " and ";
        }
    }
}
