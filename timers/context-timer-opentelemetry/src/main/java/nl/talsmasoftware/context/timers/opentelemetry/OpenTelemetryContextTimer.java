/*
 * Copyright 2016-2024 Talsma ICT
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
package nl.talsmasoftware.context.timers.opentelemetry;

import nl.talsmasoftware.context.api.ContextTimer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpenTelemetryContextTimer implements ContextTimer {
    private static final Logger LOGGER = Logger.getLogger(OpenTelemetryContextTimer.class.getName());
    private static final String INSTRUMENTATION_SCOPE = "nl.talsmasoftware.context";
    private static final String INSTRUMENTATION_VERSION = readVersion();

    @Override
    public void update(Class<?> type, String method, long duration, TimeUnit unit) {
//        GlobalOpenTelemetry.getTracer()
    }

    private static String readVersion() {
        final String path = "/META-INF/maven/nl.talsmasoftware.context.timers/context-timer-opentelemetry/pom.properties";
        try (InputStream stream = OpenTelemetryContextTimer.class.getResourceAsStream(path)) {
            Properties properties = new Properties();
            properties.load(stream);
            return properties.getProperty("version");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e, () -> "Error obtaining version from build metadata (" + path + "): " + e.getMessage());
            return null;
        }
    }
}
