[![Released Version][maven-img]][maven] 

# Context propagation (Java 8)

- Wrappers around the standard Java 8 functional to be used with context snapshots.
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

## Functional interfaces

The following functional interfaces have a `..WithContext` wrapper:
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

  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/context-propagation-java8.svg
  [maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22nl.talsmasoftware.context%22%20AND%20a%3A%22context-propagation-java8%22
