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
package nl.talsmasoftware.context.managers.opentracing;

import io.opentracing.ScopeManager;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.GlobalTracerTestUtil;
import io.opentracing.util.ThreadLocalScopeManager;
import nl.talsmasoftware.context.api.ContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * <a href="https://github.com/talsma-ict/context-propagation/issues/30">Issue 30</a> is for a
 * possible {@code NullPointerException} in {@code ScopeContext.close()}.
 * <p>
 * <a href="https://github.com/hanson76">hanson76</a> wrote:
 * <blockquote>
 * span can be {@code null} in {@code ScopeContext} if {@code OpentracingSpanManager} is used with
 * ContextAware* when there is no active span.<br>
 * The problem is that {@code ContextSnapshot.capture()} only stores
 * {@code activeContext.getValue()} which is {@code null}<br>
 * {@code ContextSnapshot.reactivate()} then retreives {@code null} from the snapshot and
 * calls {@code OpentracingSpanManger.initializeNewContext(null)}
 *
 * <p>
 * The test in ScopeContext.close only checks that closed is false before calling span.close.
 *
 * <p>
 * The fix could be to set closed to true in initializeNewContext() if span is null,
 * or add a nullcheck in SpanContext.close.
 * </blockquote>
 *
 * @author Sjoerd Talsma
 */
public class Issue30Test {
    static final ScopeManager SCOPE_MANAGER = new ThreadLocalScopeManager();

    MockTracer mockTracer;

    @BeforeEach
    public void registerMockGlobalTracer() {
        GlobalTracerTestUtil.resetGlobalTracer();
        assertThat("Pre-existing GlobalTracer", GlobalTracer.isRegistered(), is(false));
        GlobalTracer.register(mockTracer = new MockTracer(SCOPE_MANAGER));
    }

    @AfterEach
    public void cleanup() {
        GlobalTracerTestUtil.resetGlobalTracer();
    }

    @Test
    public void testIssue30NullPointerException() {
        ContextSnapshot snapshot = ContextSnapshot.capture();
        ContextSnapshot.Reactivation reactivation = snapshot.reactivate();
        reactivation.close(); // This throws NPE in issue 30
    }

}
