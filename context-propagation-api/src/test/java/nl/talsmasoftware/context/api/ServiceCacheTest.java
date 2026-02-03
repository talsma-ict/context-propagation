/*
 * Copyright 2016-2026 Talsma ICT
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
        assertThat(constructor.canAccess(null)).isFalse();
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
