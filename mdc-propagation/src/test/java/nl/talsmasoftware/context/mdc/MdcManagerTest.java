package nl.talsmasoftware.context.mdc;

import nl.talsmasoftware.context.executors.ContextAwareExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for the {@link MdcManager}.
 *
 * @author Sjoerd Talsma
 */
public class MdcManagerTest {

    ExecutorService threadpool;

    @Before
    public void setupThreadpool() {
        threadpool = new ContextAwareExecutorService(Executors.newCachedThreadPool());
    }

    @After
    public void shutdownThreadpool() {
        threadpool.shutdown();
        threadpool = null;
    }

    private static final Callable<String> GET_MDC_ITEM = new Callable<String>() {
        public String call() {
            return MDC.get("mdc-item");
        }
    };

    @Test
    public void testMdcItemPropagation() throws ExecutionException, InterruptedException {
        MDC.put("mdc-item", "Item value");
        Future<String> itemValue = threadpool.submit(GET_MDC_ITEM);
        assertThat(itemValue.get(), is("Item value"));
    }

}
