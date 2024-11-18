[![Maven Version][maven-img]][maven]
[![OpenTelemetry Badge][opentelemetry-img]][opentelemetry]

# OpenTelemetry context propagation library

Context Manager that delegates ThreadLocal management to the
default OpenTelemetry Context storage.

Obtaining the current context value is delegated to
`io.opentelemetry.context.Context.current()`.
Initializing a new context value is delegated to
`io.opentelemetry.context.Context.makeCurrent()`.

Adding the `context-manager-opentelemetry` jar to the classpath
is all that is needed to include the OpenTelemetry `Context` in ContextSnapshots.
This propagates the context to other threads using the
`ContextAwareExecutorService` or `ContextAwareCompletableFuture`.

Also, any function _..WithContext_ in the `core.function` package
automatically activates the context snapshot around the function body.

## How to use this library

Add it to your classpath.

```xml
<dependency>
    <groupId>nl.talsmasoftware.context.managers</groupId>
    <artifactId>context-manager.opentelemetry</artifactId>
    <version>[see maven-central badge above]</version>
</dependency>
```

Done!

[maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/context-propagation

[maven]: https://search.maven.org/artifact/nl.talsmasoftware.context.managers/context-manager-opentelemetry

[opentelemetry-img]: https://img.shields.io/badge/OpenTelemetry-enabled-blue.svg

[opentelemetry]: https://opentelemetry.io/
