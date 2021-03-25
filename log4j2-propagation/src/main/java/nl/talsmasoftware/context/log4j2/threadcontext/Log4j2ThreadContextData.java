/*
 * Copyright 2016-2021 Talsma ICT
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
package nl.talsmasoftware.context.log4j2.threadcontext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;

/**
 * Snapshot of the data from the Log4j 2 {@link ThreadContext} of a
 * specific thread at a certain point in the past.
 */
public class Log4j2ThreadContextData {
    private final Map<String, String> contextMap;
    private final List<String> contextStack;

    /**
     * Applies the data to the current thread.
     *
     * @param data data to apply, may be {@code null}
     * @param overwrite whether all existing data should overwritten
     */
    static void applyToCurrentThread(Log4j2ThreadContextData data, boolean overwrite) {
        if (overwrite) {
            ThreadContext.clearAll();
        }
        if (data != null) {
            data.applyToCurrentThread();
        }
    }

    /**
     * Applies the data on top of the {@code ThreadContext} of the current thread.
     * Conflicting entries are overwritten but all other existing data is kept.
     */
    void applyToCurrentThread() {
        ThreadContext.putAll(contextMap);

        // There is currently no method for pushing a collection, therefore have to
        // push one by one
        for (String element : contextStack) {
            ThreadContext.push(element);
        }
    }

    private Log4j2ThreadContextData(Map<String, String> contextMap, List<String> contextStack) {
        this.contextMap = Collections.unmodifiableMap(contextMap);
        this.contextStack = Collections.unmodifiableList(contextStack);
    }

    /**
     * Captures the {@code ThreadContext} data of the current thread.
     *
     * @return data representing the {@code ThreadContext} of the current thread
     */
    static Log4j2ThreadContextData fromCurrentThreadContext() {
        // Get a copy of context map and context stack
        return new Log4j2ThreadContextData(ThreadContext.getContext(), ThreadContext.getImmutableStack().asList());
    }

    /**
     * Returns an unmodifiable view of the {@code ThreadContext} map contained
     * in this snapshot.
     *
     * @return {@code ThreadContext} map contained in this snapshot
     */
    public Map<String, String> getContextMap() {
        return contextMap;
    }

    /**
     * Returns an unmodifiable view of the {@code ThreadContext} stack contained
     * in this snapshot. The elements are ordered from least recently added at
     * the beginning of the list to most recently added at the end.
     *
     * @return {@code ThreadContext} stack contained in this snapshot
     */
    public List<String> getContextStack() {
        return contextStack;
    }

    @Override
    public String toString() {
        return "Log4j2ThreadContextData{"
            + "map=" + contextMap
            + ",stack=" + contextStack
            + '}';
    }
}
