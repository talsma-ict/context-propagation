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

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.ThreadContext;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.clearable.ClearableContextManager;

/**
 * Manager to propagate the Log4j 2 {@link ThreadContext} data from one thread to another.
 * <p>
 * As {@code ThreadContext} already manages its own thread-local state,
 * getting the active context is delegated to the {@code ThreadContext}.
 * This means that closing the resulting context from {@link #getActiveContext()} will have
 * no effect, because its data is not managed by this library. However, calling
 * {@link #clear()} will clear the {@code ThreadContext} data of the current thread.<br>
 * Methods of this manager may have no effect when the {@code ThreadContext} has been disabled
 * (see <a href="https://logging.apache.org/log4j/2.x/manual/thread-context.html#Configuration">Log4j 2 manual</a>).
 * <p>
 * Initializing a new context through {@link #initializeNewContext(Log4j2ThreadContextData)} will
 * add the context data on top of the existing one, if any: {@code ThreadContext} stack values
 * are pushed on top of the existing stack; map entries are added to the existing map, only
 * replacing existing ones in case of a map key conflict.<br>
 * Closing a context returned from {@link #initializeNewContext(Log4j2ThreadContextData)} will reset
 * the {@code ThreadContext} to the values it had before the context was created.<br>
 * This means that closing nested contexts out-of-order will probably result in an undesirable state.<br>
 * It is therefore strongly advised to use Java's {@code try-with-resources} statement to ensure proper
 * closing when nesting {@code ThreadContext} contexts.
 * <p>
 * This manager and the contexts it creates are not suited to create {@code ThreadContext} 'scopes'
 * within the same thread. Log4j 2's {@link CloseableThreadContext} should be used for that instead.
 * <p>
 * Log4j 2 supports making {@code ThreadContext} inheritable, i.e. to have it use
 * {@link InheritableThreadLocal}. In some cases this might solve the problem of propagating
 * context from one thread to another. However in all cases where threads are reused (such
 * as thread pool executors) this will not work. It is therefore recommended to always
 * prefer using this library to cover all use cases.
 * <p>
 * As with all manager implementations of this library there is usually no need to directly
 * interact with the manager classes. Instead Java's {@link ServiceLoader} makes sure they
 * are loaded as services. If an instance of this class is needed nonetheless it can be obtained
 * through the field {@link #INSTANCE}.
 *
 * @see <a href="https://logging.apache.org/log4j/2.x/manual/thread-context.html">Log4j 2 Thread Context manual</a>
 */
public class Log4j2ThreadContextManager implements ClearableContextManager<Log4j2ThreadContextData> {
    /**
     * Singleton instance of this class.
     */
    public static final Log4j2ThreadContextManager INSTANCE = new Log4j2ThreadContextManager();

    /**
     * Returns the singleton instance.
     * <p>
     * This method mainly exists for usage by {@link ServiceLoader}. The singleton instance
     * can also directly be obtained from {@link #INSTANCE}.
     *
     * @return {@link #INSTANCE}
     */
    // ServiceLoader supports "provider" method since Java 9
    public static Log4j2ThreadContextManager provider() {
        return INSTANCE;
    }

    /**
     * Creates a new context manager.
     *
     * @deprecated
     *      This constructor only exists for usage by {@link ServiceLoader}. The singleton instance
     *      obtained from {@link #INSTANCE} should be used instead.
     */
    @Deprecated
    public Log4j2ThreadContextManager() {
    }

    /**
     * Initializes a new context for the Log4j 2 {@link ThreadContext} of the current thread.
     * The data of the context is applied on top of existing data (if any) only replacing
     * conflicting {@code ThreadContext} map entries but keeping all other existing data.
     *
     * @param value non-{@code null} data for the {@code ThreadContext}
     * @return The new <em>active</em> context containing the specified value
     * which should be closed by the caller at the end of its lifecycle from the same thread.
     */
    public Context<Log4j2ThreadContextData> initializeNewContext(final Log4j2ThreadContextData value) {
        if (value == null) {
            throw new NullPointerException("value must not be null");
        }

        // Capture current ThreadContext as 'previous' and make the given data the 'new current' ThreadContext
        Log4j2ThreadContextData previous = Log4j2ThreadContextData.fromCurrentThreadContext();
        Log4j2ThreadContextData.applyToCurrentThread(value, false); // Add ThreadContext data on top of existing
        return new ThreadContextContext(previous, value, false);
    }

    /**
     * Returns a context consisting of the active Log4j 2 {@link ThreadContext} data from the current thread.
     * <p>
     * <strong>Please note:</strong> <em>Because these values are managed by Log4j 2 itself and not
     * by this library, closing the resulting context has no effect.</em>
     *
     * @return Context containing the active Log4j 2 {@code ThreadContext} data
     */
    public Context<Log4j2ThreadContextData> getActiveContext() {
        // Return fresh context that is 'already-closed'. Therefore it doesn't need previous ThreadContext data
        return new ThreadContextContext(null, Log4j2ThreadContextData.fromCurrentThreadContext(), true);
    }

    /**
     * Clears the current Log4j 2 {@code ThreadContext} of the calling thread.
     */
    public void clear() {
        Log4j2ThreadContextData.applyToCurrentThread(null, true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private static final class ThreadContextContext implements Context<Log4j2ThreadContextData> {
        private final Log4j2ThreadContextData previous, value;
        private final AtomicBoolean closed;

        private ThreadContextContext(Log4j2ThreadContextData previous, Log4j2ThreadContextData value, boolean closed) {
            this.previous = previous;
            this.value = value;
            this.closed = new AtomicBoolean(closed);
            ContextManagers.onActivate(Log4j2ThreadContextManager.class, value, previous);
        }

        public Log4j2ThreadContextData getValue() {
            return value;
        }

        public void close() {
            if (closed.compareAndSet(false, true)) {
                Log4j2ThreadContextData.applyToCurrentThread(previous, true); // Restore previous; overwrite current ThreadContext
                ContextManagers.onDeactivate(Log4j2ThreadContextManager.class, value, previous);
            }
        }

        @Override
        public String toString() {
            return closed.get() ? "ThreadContextContext{closed}" : "ThreadContextContext{" + value + '}';
        }
    }
}
