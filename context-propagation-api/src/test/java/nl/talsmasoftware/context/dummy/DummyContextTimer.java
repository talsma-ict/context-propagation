package nl.talsmasoftware.context.dummy;

import nl.talsmasoftware.context.api.ContextTimer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DummyContextTimer implements ContextTimer {
    private static final Map<String, Long> LAST_TIMED = new HashMap<String, Long>();

    public static Long getLastTimedMillis(Class<?> type, String method) {
        return LAST_TIMED.get(type.getName() + "." + method);
    }

    public void update(Class<?> type, String method, long duration, TimeUnit unit, Throwable error) {
        LAST_TIMED.put(type.getName() + "." + method, unit.toMillis(duration));
    }

    public static void clear() {
        LAST_TIMED.clear();
    }
}
