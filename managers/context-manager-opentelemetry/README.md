[![Maven Version][maven-img]][maven]
[![OpenTelemetry Badge][opentelemetry-img]][opentelemetry]

# OpenTelemetry context propagation library

Context Manager for the opentelemetry `io.opentelemetry.context.Context`.

Includes the `current` opentelemetry `Context` in _captured_ `ContextSnapshot`s.

The ContextManager delegates ThreadLocal management to the configured OpenTelemetry `ContextStorage`.  
Obtaining the current context value is delegated to `io.opentelemetry.context.Context.current()`.  
Initializing a new context value is delegated to `io.opentelemetry.context.Context.makeCurrent()`.

## Bridge function

Besides capturing the current Context, this module also adds an `OpenTelemetryContextStorageWrapper`
to the opentelemetry `ContextStorage`.

This wrapper includes captured `ContextSnapshot`s into each Context
returned from `io.opentelemetry.context.Context.current()`,
thereby bridging _all_ supported `ContextManager` implementations over the
`io.opentelemetry.context.Context` mechanism.

## Bridge example: Spring-security

Suppose your application uses `spring-security` to store the current `Authentication` data.
Also, opentelemetry is used for instrumentation purposes.

Then you can:

- replace `spring-security` dependency by `context-manager-spring-security` (which already includes spring-security for
  you as transitive dependency).
- replace `opentelemetry-context` dependency by `context-manager-opentelemetry` (which already includes
  opentelemetry-context for you as transitive dependency).

This will automatically:

1. capture and reactivate the current spring-security `Authentication` _and_ opentelemetry `Context` in
   `ContextSnapshot`s.
2. capture and reactivate the current spring-security `Authentication` in opentelemetry `Context`s.
3. propagate any additional contexts managed by a supported ContextManager through both these mechanisms.

## Usage

Adding the `context-manager-opentelemetry` jar to the classpath
is all that is needed to include the OpenTelemetry `Context` in ContextSnapshots
and enable the bridge function.

The utility classes such as `ContextAwareExecutorService` or `ContextAwareCompletableFuture`
will automatically support opentelemetry Contexts then.

Also, any function _..WithContext_ in the `core.function` package
automatically activates the context snapshot around the function body.

Add the following dependency to your classpath:

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
