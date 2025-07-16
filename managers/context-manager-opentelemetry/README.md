[![Maven Version][maven-img]][maven]
[![OpenTelemetry Badge][opentelemetry-img]][opentelemetry]

# OpenTelemetry context propagation library

Context Manager for the open telemetry `io.opentelemetry.context.Context`.

Includes the `current` open telemetry `Context` in _captured_ `ContextSnapshot`s.

The ContextManager delegates ThreadLocal management to the configured OpenTelemetry `ContextStorage`.  
Obtaining the current context value is delegated to `io.opentelemetry.context.Context.current()`.  
Initializing a new context value is delegated to `io.opentelemetry.context.Context.makeCurrent()`.

## Bridge function

Besides capturing the current Context, this module also adds an `OpenTelemetryContextStorageWrapper`
to the configured open telemetry `ContextStorage`.

This wrapper includes captured `ContextSnapshot`s into each Context
returned from `io.opentelemetry.context.Context.current()`,
thereby bridging _all_ supported `ContextManager` implementations over the
`io.opentelemetry.context.Context` mechanism.

## How to use this library

Adding the `context-manager-opentelemetry` jar to the classpath
is all that is needed to include the OpenTelemetry `Context` in ContextSnapshots
and enable the bridge function.

This includes support by the utility classes such as
`ContextAwareExecutorService` or `ContextAwareCompletableFuture`.

Also, any function _..WithContext_ in the `core.function` package
automatically activates the context snapshot around the function body.

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
