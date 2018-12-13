[![Released Version][maven-img]][maven] 

# Context propagation (Java 8)

- Wrappers around the standard Java 8 functional interfaces to use with a context snapshot.
- A `ContextAwareCompletableFuture` implementation.

## How to get this module

Add the following dependency to your classpath:
```xml
<dependency>
    <groupId>nl.talsmasoftware.context</groupId>
    <artifactId>context-propagation-java8</artifactId>
    <version>[see maven-central version above]</version>
</dependency>
```

## Applicability

By far the easiest use of the context propagation libary is using the
[`ContextAwareExecutorService`][ContextAwareExecutorService].

If that is not a possibility, the following java functional interface wrappers
in this java 8 module may be of help to you:

## Functional interfaces

The easiest use of the context propagation libary is using the
[`ContextAwareExecutorService`][ContextAwareExecutorService].

If that is not a possibility, you can use the following java functional interface wrappers
that will apply a captured context snapshot to the wrapped function:

- [`BiConsumer`](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation-java8/latest/nl/talsmasoftware/context/functions/BiConsumerWithContext.html)
- [`BiFunction`](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation-java8/latest/nl/talsmasoftware/context/functions/BiFunctionWithContext.html)
- [`BinaryOperator`](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation-java8/latest/nl/talsmasoftware/context/functions/BinaryOperatorWithContext.html)
- [`BiPredicate`](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation-java8/latest/nl/talsmasoftware/context/functions/BiPredicateWithContext.html)
- [`BooleanSupplier`](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation-java8/latest/nl/talsmasoftware/context/functions/BooleanSupplierWithContext.html)
- [`Consumer`](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation-java8/latest/nl/talsmasoftware/context/functions/ConsumerWithContext.html)
- [`Function`](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation-java8/latest/nl/talsmasoftware/context/functions/FunctionWithContext.html)
- [`Predicate`](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation-java8/latest/nl/talsmasoftware/context/functions/PredicateWithContext.html)
- [`Runnable`](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation-java8/latest/nl/talsmasoftware/context/functions/RunnableWithContext.html)
- [`Supplier`](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation-java8/latest/nl/talsmasoftware/context/functions/SupplierWithContext.html)
- [`UnaryOperator`](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation-java8/latest/nl/talsmasoftware/context/functions/UnaryOperatorWithContext.html)


## ContextAwareCompletableFuture

The [`CompletableFuture`][CompletableFuture] class was introduced in Java 8:
> _A `Future` that may be explicitly completed (setting its value and status),
> and may be used as a [`CompletionStage`][CompletionStage],
> supporting dependent functions and actions that trigger upon its completion._

The _context-aware_ completable future will propagate the context into most completion stage
operations, in particular:
- `thenApply` and its _async_ variants
- `thenAccept` and its _async_ variants
- `thenRun` and its _async_ variants
- `thenCombine` and its _async_ variants
- `thenAcceptBoth` and its _async_ variants
- `thenCompose` and its _async_ variants
- `whenComplete` and its _async_ variants
- `handle` and its _async_ variants
- `exceptionally`
- `runAfterBoth` and its _async_ variants
- `applyToEither` and its _async_ variants
- `acceptEither` and its _async_ variants
- `runAfterEither` and its _async_ variants

Furthermore, when the order of stages is clearly defined (e.g. the _thenXyz_, _whenComplete_ etc),
a new context snapshot will be taken between each stage, allowing for context changes from one stage
to the next.

The [`ContextAwareCompletableFutureTest`](https://github.com/talsma-ict/context-propagation/blob/develop/context-propagation-java8/src/test/java/nl/talsmasoftware/context/futures/ContextAwareCompletableFutureTest.java) class contains
test cases that demonstrate the behaviour of this complex class.

### Caveats

Please be aware that:
1. in almost all circumstances it's preferrable 
   to choose `ContextAwareExecutorService` over `ContextAwareCompletableFuture`.
2. neither the standard `CompletableFuture` nor this context-aware version
   will attempt to cancel or interrupt an ongoing process when cancelled.
3. when traversing completion stages, context snapshots are taken
   which may or may not be needed for the next completion stage 
   (ref [Issue 85](https://github.com/talsma-ict/context-propagation/issues/85)).
   Depending on the operation, this could be costly.

  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/context-propagation-java8.svg
  [maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22nl.talsmasoftware.context%22%20AND%20a%3A%22context-propagation-java8%22

  [ContextAwareExecutorService]: https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/executors/ContextAwareExecutorService.html
  [CompletableFuture]: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html
  [CompletionStage]: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html
  
