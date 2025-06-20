[![Maven Version][maven-img]][maven]
[![Javadoc][javadoc-img]][javadoc]
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=talsma-ict_context-propagation&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=talsma-ict_context-propagation)
[![Coverage Status][coveralls-img]][coveralls]

> [!IMPORTANT]
> See [what's new in version 2](#new-in-version-2).

# Context propagation library

Library to capture a snapshot of one or more `ThreadLocal` values and reactivate them in another thread.

The library automatically detects supported `ThreadLocal` values to be captured.
It uses a `ContextManager` Service Provider Interface (SPI) for this using the Java [ServiceLoader].

The core module provides several [utility classes](#utility-classes) to safely capture a context snapshot
in the calling thread and reativating it in another thread and ensuring proper
cleanup after its execution finishes. This reduces the chance of 'leaking' thread-local
values.

## API concepts

A brief explanation of the core concepts from the api:

### ContextSnapshot

Captures all active values from detected [ContextManager](#contextmanager) implementations.

These values can be _reactivated_ in another thread.  
Reactivated snapshots **must be closed** to avoid leaking context.

All _context aware_ [utility classes](#utility-classes) in this library are tested
to make sure they reactivate _and_ close snapshots in a safe way.

### ContextManager

Manages a [context](#context) by providing a standard way of interacting with `ThreadLocal` values.

Thread-local values can be accessed via a ContextManager by:

- Calling `getActiveContextValue()` which _gets_ the current thread-local value.
- Calling `activate(value)` which _sets_ the given value until `close()` is called on the resulting `Context`.
- Calling `clear()` which _removes_ the thread-local value.

### Context

Abstraction for an _activated_ thread-local value.

When the context manager activates a value, a new `Context` is returned.
_Closing_ this context will undo this activated value again.

> [!IMPORTANT]
> It is the responsibility of the one activating a new Context to also close it _from the same thread_.
> Using every activated context in a 'try-with-resources' block of code is a recommended and safe way to do this.

## Utility classes

The `context-propagation-core` module contains various utility classes 
that make it easier to capture context snapshots and reactivate them safely in other threads.

Examples include:

- [`ContextAwareExecutorService`][ContextAwareExecutorService], wrapping any existing `ExecutorService`,
  automatically capturing and reactivating context snapshots.
- [`ContextAwareCompletableFuture`][ContextAwareCompletableFuture],
  propagating context snapshots into each successive `CompletionStage`.
- Variants of java standard `java.util.function` implementations,
  executing within a context snapshot.
- Base class `AbstractThreadLocalContext` that features nesting active values and predictable behaviour for out-of-order closing. 

## How to use this library

### Threadpools and ExecutorService

If your background threads are managed by an ExecutorService,
you can use our _context aware_ ExecutorService to wrap your usual threadpool.

The `ContextAwareExcutorService` automatically captures context snapshots before submitting work. 
This snapshot is then reactivated (and closed) in the submitted background thread.  

It can wrap any ExecutorService for the actual thread execution:

```java
private static final ExecutorService THREADPOOL =
        ContextAwareExecutorService.wrap(Executors.newCachedThreadpool());
```

### Manually capture and reactivate a context snapshot

Just before creating a new thread, capture a snapshot of all ThreadLocal context
values:

```java
final ContextSnapshot snapshot = ContextSnapshot.capture();
```

In the code of your background thread, reactivate the snapshot to have all ThreadLocal
context values set as they were captured:

```java
try (ContextSnapshot.Reactivation reactivation = snapshot.reactivate()) {
    // All ThreadLocal values from the snapshot are available within this block
}

// or, using a Runnable lambda:
snapshot.wrap(() -> {
    // All ThreadLocal values from the snapshot are available within this block
}).run();
```

## Supported contexts

The following `ThreadLocal`-based contexts are currently supported
out of the box by this context-propagation library:

- [SLF4J MDC (Mapped Diagnostic Context)][slf4j mdc propagation]
- [Log4j 2 Thread Context][log4j2 thread context propagation]
- [OpenTelemetry Context][opentelemetry context propagation]
- [OpenTracing Span contexts][opentracing span propagation]
- [Spring Security Context]
- [Locale context][locale context]
- [ServletRequest contexts][servletrequest propagation]
- .. _Yours?_
  Feel free to create an issue or pull-request if you know of
  a ThreadLocal context that should also be included in a context snapshot.

## Custom contexts

Adding your own `Context` type is possible
by [creating your own context manager](context-propagation-api/README.md#creating-your-own-context-manager).

## Building jars with dependencies

When using a build tool or plugin to create an 'uber-jar', i.e. a jar file with all
the classes of its dependencies included, you have to make sure that the service
provider configuration files under `META-INF/services` are either preserved or
merged. Otherwise Java's `ServiceLoader` will not be able to find the context
implementations of this library.

In case you are using the Maven Shade Plugin, you can use the
[
`ServicesResourceTransformer`](https://maven.apache.org/plugins/maven-shade-plugin/examples/resource-transformers.html#ServicesResourceTransformer)
for this task.

## Performance metrics

No library is 'free' with regards to performance.
Capturing a context snapshot and reactivating it in another thread is no different.
For insight, the library tracks the overall time used creating and reactivating
context snapshots along with time spent in each individual `ContextManager`.

### Logging performance

On a development machine, you can get timing for each snapshot by turning on logging
for `nl.talsmasoftware.context.api.ContextTimer` at `FINEST` or `TRACE` level
(depending on your logger of choice).
Please **do not** turn this on in production as the logging overhead will most likely
have a noticeable impact on your application.

### Metrics reporting

The [context propagation metrics] module uses the excellent
[dropwizard metrics](https://metrics.dropwizard.io/) library to
instrument Timers for context propagation.

Similarly, the [context propagation Micrometer] module adds [Micrometer]
instrumentation Timers for the context propagation.

Adding either of these modules to your classpath will automatically
configure various timers in the global default metric registry of your application.

## New in version 2

Purpose of 'v2' of this library has been simplification of both the API
and the structure of the repository.

- Minimum Java version bumped to 8.
- Repository module restructuring.
  - Separate API module containing only the minimum API.
  - Separate core module for all [utility classes](#utility-classes).
  - All provided manager implementations moved to `managers` subdirectory.
  - All context timer implementations moved to 'timers' subdirectory.
- API simplification. 
  - Static `ContextSnapshot.capture()` captures a new snapshot and `ContextSnapshot.reactivate()` reactivates it.
  - `ContextManager.initializeNewContext(value)` was renamed to `activate(value)`.
  - `ContextManager.getActiveContext()` was replaced by `getActiveContextValue()`.
  - `ContextManager.clear()` must be implemented, but is allowed to be a 'no-op' empty implementation. The `Clearable` interface was removed.
- All `@Deprecated(forRemoval=true)` items from v1 were removed.

## License

[Apache 2.0 license](LICENSE)


[maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/context-propagation

[maven]: https://search.maven.org/search?q=g:nl.talsmasoftware.context

[release-img]: https://img.shields.io/github/release/talsma-ict/context-propagation.svg

[release]: https://github.com/talsma-ict/context-propagation/releases

[coveralls-img]: https://coveralls.io/repos/github/talsma-ict/context-propagation/badge.svg

[coveralls]: https://coveralls.io/github/talsma-ict/context-propagation

[javadoc-img]: https://www.javadoc.io/badge/nl.talsmasoftware.context/context-propagation.svg

[javadoc]: https://www.javadoc.io/doc/nl.talsmasoftware.context/context-propagation


[locale context]: managers/context-manager-locale

[log4j2 thread context propagation]: managers/context-manager-log4j2

[opentelemetry context propagation]: managers/context-manager-opentelemetry

[opentracing span propagation]: managers/context-manager-opentracing

[serviceloader]: https://docs.oracle.com/javase/8/docs/api/index.html?java/util/ServiceLoader.html

[servletrequest propagation]: managers/context-manager-servletrequest

[slf4j mdc propagation]: managers/context-manager-slf4j

[spring security context]: managers/context-manager-spring-security

[context propagation metrics]: timers/context-timer-metrics

[context propagation micrometer]: timers/context-timer-micrometer

[micrometer]: https://micrometer.io

[ContextAwareExecutorService]: https://javadoc.io/doc/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/core/concurrent/ContextAwareExecutorService.html

[ContextAwareCompletableFuture]: context-propagation-core/README.md#contextawarecompletablefuture
