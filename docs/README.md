[![Build Status][ci-img]][ci]
[![Coverage Status][coveralls-img]][coveralls]
[![Maven Version][maven-img]][maven]

# Context propagation library

Library to propagate `ThreadLocal` snapshots by reactivating in another thread.

Tools help automate propagation by capturing snapshots 
and ensuring proper closing of reactivated context snapshots:
- [`ContextAwareExecutorService`][ContextAwareExecutorService] that wraps any existing `ExecutorService`
- [`ContextAwareCompletableFuture`][ContextAwareCompletableFuture] that 
  propagates context snapshots into each successive `CompletionStage`.

## Terminology

### Context

A context contains a value. There is one _active_ context _per thread_.  
An `AbstractThreadLocalContext` base class is provided that allows nested contexts 
and provides predictable behaviour for out-of-order closing.

### ContextManager

Manages the active context.  
Can initialize a new context and provides to the active context.

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

If your background threads are managed by an `ExecutorService` acting as a threadpool,
you can use the `ContextAwareExecutorService` instead of your usual threadpool.  
This automatically takes a new context snapshot when submitting new work
and reactivates this snapshot in the background thread.  
The `ContextAwareExecutorService` can wrap any `ExecutorService` for the actual thread execution:
```java
// private static final ExecutorService THREADPOOL = Executors.newCachedThreadpool();
private static final ExecutorService THREADPOOL = 
        new ContextAwareExecutorService(Executors.newCachedThreadpool());
```

It will automatically create a snapshot and reactivate it in the 
background thread when started.  
The ThreadLocal values from the calling thread will therefore 
be available in the background thread as well.

## Supported contexts

The following `ThreadLocal`-based contexts are currently supported 
out of the box by this context-propagation library:

- [Slf4J MDC (Mapped Diagnostic Context)][mdc propagation]
- [OpenTracing Span contexts][opentracing span propagation]
- [Spring Security Context]
- [Locale context][locale context]
- [ServletRequest contexts][servletrequest propagation]
- _Yours?_ Feel free to create an issue or pull-request
  if you believe there's a general context that was forgotten. 

## Custom contexts

Adding your own `Context` type is possible
by [creating your own context manager](../context-propagation-java5/README.md#creating-your-own-context-manager).

## Caching

By default the `ContextManagers` class caches the context manager instances it finds
per _classloader_.
Since the cache is per classloader, this should work satisfactory
for applications with simple classloader hierarchies (e.g. _dropwizard_) 
and also for complex hierarchies (_spring boot_, _JEE_ and the like).

### Disable caching

If however, you wish to disable caching of the context manager instances, you can set either:
- the java system property `talsmasoftware.context.caching`, or
- the environment variable `TALSMASOFTWARE_CONTEXT_CACHNG`

The values `false` or `0` will _disable_ caching.

## Performance metrics

No library is 'free' with regards to performance.
Capturing a context snapshot and reactivating it in another thread is no different.
For insight, the library tracks the overall time used creating and reactivating
context snapshots along with time spent in each individual `ContextManager`.

### Logging performance

On a development machine, you can get timing for each snapshot by turning on logging
for `nl.talsmasoftware.context.Timing` at `FINEST` or `TRACE` level 
(depending on your logger of choice).
Please **do not** turn this on in production as the logging overhead will most likely
have a noticable impact on your application.

### Metrics reporting

The [context propagation metrics] module supports for the excellent
[dropwizard metrics](https://metrics.dropwizard.io/) library.  
Adding `context propagation metrics` to your classpath will automatically 
configure various timers in the default metric registry of your application.

## License

[Apache 2.0 license](../LICENSE)


  [ci-img]: https://travis-ci.org/talsma-ict/context-propagation.svg?branch=develop
  [ci]: https://travis-ci.org/talsma-ict/context-propagation
  [maven-img]: https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/nl/talsmasoftware/context/context-propagation/maven-metadata.xml.svg
  [maven]: http://mvnrepository.com/artifact/nl.talsmasoftware.context
  [release-img]: https://img.shields.io/github/release/talsma-ict/context-propagation.svg
  [release]: https://github.com/talsma-ict/context-propagation/releases
  [coveralls-img]: https://coveralls.io/repos/github/talsma-ict/context-propagation/badge.svg
  [coveralls]: https://coveralls.io/github/talsma-ict/context-propagation

  [servletrequest propagation]: ../servletrequest-propagation
  [mdc propagation]: ../mdc-propagation
  [locale context]: ../locale-context
  [spring security context]: ../spring-security-context
  [opentracing span propagation]: ../opentracing-span-propagation
  [context propagation metrics]: ../context-propagation-metrics
  [default constructor]: https://en.wikipedia.org/wiki/Nullary_constructor
  
  [ContextAwareExecutorService]: https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/executors/ContextAwareExecutorService.html
  [ContextAwareCompletableFuture]: ../context-propagation-java8#contextawarecompletablefuture
