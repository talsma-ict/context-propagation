package nl.talsmasoftware.context.api;

import nl.talsmasoftware.context.dummy.DummyContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Isolated("Tests influence service cache.")
class ServiceCacheTest {
    ConcurrentMap<Class, List> cache = ServiceCacheTestUtil.getInternalCacheMap();

    @BeforeEach
    @AfterEach
    void clearCache() {
        ServiceCache.useClassLoader(null);
        ServiceCache.clear();
    }

    @Test
    void serviceCacheIsUtilityClass() throws ReflectiveOperationException {
        Constructor<ServiceCache> constructor = ServiceCache.class.getDeclaredConstructor();
        assertThat(constructor.isAccessible()).isFalse();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .cause().isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void useClassLoader_clearsCache() {
        ServiceCache.cached(ContextManager.class);
        assertThat(cache).isNotEmpty();

        ServiceCache.useClassLoader(getClass().getClassLoader());
        assertThat(cache).isEmpty();

        ServiceCache.cached(ContextManager.class);
        assertThat(cache).isNotEmpty();
    }

    @Test
    void findContextManager_nullReturnsNull() {
        assertThat(ServiceCache.findContextManager(null)).isNull();
    }

    @Test
    void findContextManager_returnsNullIfNoManagerFound() {
        cache.clear();
        assertThat(ServiceCache.findContextManager(ContextManager.class.getName())).isNull();
        assertThat(cache).isNotEmpty();
    }

    @Test
    void findContextManager_returnsManagerIfFound() {
        assertThat(ServiceCache.findContextManager(DummyContextManager.class.getName()))
                .isNotNull()
                .isInstanceOf(DummyContextManager.class);
    }
}
