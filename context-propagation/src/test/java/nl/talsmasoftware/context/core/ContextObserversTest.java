package nl.talsmasoftware.context.core;

import nl.talsmasoftware.context.DummyContextManager;
import nl.talsmasoftware.context.api.Context;
import nl.talsmasoftware.context.api.ContextManager;
import nl.talsmasoftware.context.observer.ContextObserver;
import nl.talsmasoftware.context.observer.SimpleContextObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContextObserversTest {
    static final class NoopContextManager implements ContextManager<Object> {
        public Context<Object> initializeNewContext(Object value) {
            return null;
        }

        public Context<Object> getActiveContext() {
            return null;
        }

        public Object getActiveContextValue() {
            return null;
        }

        public void clear() {
        }
    }

    @Test
    void registerContextObserver_nullObserver() {
        RuntimeException expected = assertThrows(NullPointerException.class, new Executable() {
            public void execute() {
                ContextManagers.registerContextObserver(null, DummyContextManager.class);
            }
        });
        assertThat(expected.getMessage(), is("Context observer must not be null."));
    }

    @Test
    void registerContextObserver_nullContextManagerType() {
        final ContextObserver<Object> observer = new SimpleContextObserver();
        RuntimeException expected = assertThrows(NullPointerException.class, new Executable() {
            public void execute() {
                ContextManagers.registerContextObserver(observer, null);
            }
        });
        assertThat(expected.getMessage(), is("Observed ContextManager type must not be null."));
    }

    @Test
    void registerContextObserver_forUnknownContextManagerType() {
        final ContextObserver<Object> observer = new SimpleContextObserver();

        boolean result = ContextManagers.registerContextObserver(observer, NoopContextManager.class);

        assertThat(result, is(false));
    }

    @Test
    void testConcurrentRegistrations() throws ExecutionException, InterruptedException {
        // prepare
        final ExecutorService threadPool = Executors.newFixedThreadPool(10);
        final SimpleContextObserver observer = new SimpleContextObserver();
        try {
            final Callable<Integer> registerTask = new Callable<Integer>() {
                public Integer call() {
                    return ContextManagers.registerContextObserver(observer, DummyContextManager.class) ? 1 : 0;
                }
            };
            List<Future<Integer>> results = new ArrayList<Future<Integer>>();

            // execute
            for (int i = 0; i < 100; i++) {
                results.add(threadPool.submit(registerTask));
            }

            // verify
            int sum = 0;
            for (Future<Integer> result : results) {
                sum += result.get();
            }
            assertThat(sum, is(1));
        } finally {
            ContextManagers.unregisterContextObserver(observer);
            threadPool.shutdownNow();
        }
    }

}
