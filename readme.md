[![CI Build][ci-img]][ci]
[![Coverage Status][coveralls-img]][coveralls]
[![Maven Version][maven-img]][maven]
[![Javadoc][javadoc-img]][javadoc]

> [!IMPORTANT]
> Main focus of the 'v2' version is simplification of this library.
> In the past, many features were added to the library. Some introduced more complexity than technically needed.  
> Due to semantic versioning we can't break functionality for the sake of simplicity. 
> So for v2 we must redesign some api elements that allow simpler implementation, reducing complexity.
> 
> This is  the development branch for the v2 version of this library.
> All breaking changes to the library must go in this branch.
> - Java version bumped to Java8 mimimum
> - `ContextManager.getActiveContext()` replaced by `getActiveContextValue()`.
> - `ContextManager.clear()` will require an implementation.
> - All `@Deprecated(forRemoval=true)` is to be removed.
> - Add caveat about use vs. Scoped values and Structured Concurrency.

# Context propagation library

Propagate a snapshot of one or more `ThreadLocal` values into another thread.

This library enables automatic propagation of several well-known ThreadLocal contexts 
by capturing a snapshot, reactivating it in another thread and ensuring proper 
cleanup after execution finishes:

- [`ContextAwareExecutorService`][ContextAwareExecutorService] 
  wrapping any existing `ExecutorService`.
- [`ContextAwareCompletableFuture`][ContextAwareCompletableFuture] 
  propagating context snapshots into each successive `CompletionStage`.

## Terminology

### Context

Abstraction containing a value in the context of a _thread_. 
The most common implementation in Java is a ThreadLocal value.
The library provies an `AbstractThreadLocalContext` base class 
that features nesting values and predictable behaviour for out-of-order closing.

For each context type, there can only be one _active_ context per thread at any time.

### ContextManager

Manages a context.
The ContextManager API can activate a new context value and 
provides access to the active context value.

### ContextSnapshot

A snapshot contains the current value from _all_ known context managers.  
These values can be _reactivated_ in another thread.  
Reactivated snapshots **must be closed** to avoid leaking context.  

All _context aware_ utility classes in this library are tested 
to make sure they reactivate _and_ close snapshots in a safe way.

## How to use this library

### Capturing a snapshot of ThreadLocal context values

Just before creating a new thread, capture a snapshot of all ThreadLocal context
values:
```java
ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
```

In the code of your background thread, activate the snapshot to have all ThreadLocal
context values set as they were captured:
```java
try (Context<Void> reactivation = snapshot.reactivate()) {
    // All ThreadLocal values from the snapshot are available within this block
}
```

### Threadpools and ExecutorService

If your background threads are managed by an ExecutorService,
you can use our _context aware_ ExecutorService instead of your usual threadpool.

When submitting new work, this automatically takes a context snapshot
to reactivate in the background thread.  
After the background thread finishes the snapshot is closed,
ensuring no ThreadLocal values leak into the thread pool.

The `ContextAwareExecutorService` can wrap any ExecutorService for the actual thread execution:
```java
private static final ExecutorService THREADPOOL = 
        new ContextAwareExecutorService(Executors.newCachedThreadpool());
```

## Supported contexts

The following `ThreadLocal`-based contexts are currently supported 
out of the box by this context-propagation library:

- [SLF4J MDC (Mapped Diagnostic Context)][slf4j mdc propagation]
- [Log4j 2 Thread Context][log4j2 thread context propagation]
- [OpenTracing Span contexts][opentracing span propagation]
- [Spring Security Context]
- [Locale context][locale context]
- [ServletRequest contexts][servletrequest propagation]
- _Yours?_ Feel free to create an issue or pull-request
  if you believe there's a general context that was forgotten. 

## Custom contexts

Adding your own `Context` type is possible
by [creating your own context manager](context-propagation/README.md#creating-your-own-context-manager).

## Building jars with dependencies

When using a build tool or plugin to create an 'uber-jar', i.e. a jar file with all
the classes of its dependencies included, you have to make sure that the service
provider configuration files under `META-INF/services` are either preserved or
merged. Otherwise Java's `ServiceLoader` will not be able to find the context
implementations of this library.

In case your are using the Maven Shade Plugin, you can use the
[`ServicesResourceTransformer`](https://maven.apache.org/plugins/maven-shade-plugin/examples/resource-transformers.html#ServicesResourceTransformer)
for this task.

## Performance metrics

No library is 'free' with regards to performance.
Capturing a context snapshot and reactivating it in another thread is no different.
For insight, the library tracks the overall time used creating and reactivating
context snapshots along with time spent in each individual `ContextManager`.

### Logging performance

On a development machine, you can get timing for each snapshot by turning on logging
for `nl.talsmasoftware.context.core.Timers` at `FINEST` or `TRACE` level 
(depending on your logger of choice).
Please **do not** turn this on in production as the logging overhead will most likely
have a noticable impact on your application.

### Metrics reporting

The [context propagation metrics] module uses the excellent
[dropwizard metrics](https://metrics.dropwizard.io/) library to
instrument Timers for context propagation.

Similarly, the [context propagation Micrometer] module adds [Micrometer] 
instrumentation Timers for the context propagation.

Adding either of these modules to your classpath will automatically
configure various timers in the global default metric registry of your application.

## License

[Apache 2.0 license](LICENSE)


  [ci-img]: https://github.com/talsma-ict/context-propagation/actions/workflows/ci-build.yml/badge.svg
  [ci]: https://github.com/talsma-ict/context-propagation/actions/workflows/ci-build.yml
  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/context-propagation
  [maven]: https://search.maven.org/search?q=g:nl.talsmasoftware.context
  [release-img]: https://img.shields.io/github/release/talsma-ict/context-propagation.svg
  [release]: https://github.com/talsma-ict/context-propagation/releases
  [coveralls-img]: https://coveralls.io/repos/github/talsma-ict/context-propagation/badge.svg
  [coveralls]: https://coveralls.io/github/talsma-ict/context-propagation
  [javadoc-img]: https://www.javadoc.io/badge/nl.talsmasoftware.context/context-propagation.svg
  [javadoc]: https://www.javadoc.io/doc/nl.talsmasoftware.context/context-propagation-root


  [servletrequest propagation]: servletrequest-propagation
  [slf4j mdc propagation]: slf4j-propagation
  [log4j2 thread context propagation]: log4j2-propagation
  [locale context]: locale-context
  [spring security context]: spring-security-context
  [opentracing span propagation]: opentracing-span-propagation
  [context propagation metrics]: context-propagation-metrics
  [context propagation micrometer]: context-propagation-micrometer
  [micrometer]: https://micrometer.io
  
  [ContextAwareExecutorService]: https://javadoc.io/doc/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/executors/ContextAwareExecutorService.html
  [ContextAwareCompletableFuture]: context-propagation-java8#contextawarecompletablefuture
