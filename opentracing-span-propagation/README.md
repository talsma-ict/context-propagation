[![Released Version][maven-img]][maven] 
[![OpenTracing Badge][opentracing-img]][opentracing]

# OpenTracing Span propagation library

Adding the `opentracing-span-propagation` jar to your classpath
is all that is needed to automatically obtain an active `Span` `Scope`
from the `GlobalTracer` and become part of the `ContextSnapshot`.

## How to use this library

Add it to your classpath. 
```xml
<dependency>
    <groupId>nl.talsmasoftware.context</groupId>
    <artifactId>opentracing-span-propagation</artifactId>
    <version>[see maven-central version above]</version>
</dependency>
```

Done!

Now the `GlobalTracer.get().activeSpan()` is included in each
snapshot created by the `ContextManagers.createSnapshot()` method
and reactivated with it.  
This includes all usages of the `ContextAwareExecutorService`.

_Please note:_ All snapshot `reactivate()` results **must be closed** 
(from the same thread) if the `opentracing-span-propagation` is used due to the
Opentracing `Scope` semantics, where each activated scope **must** be closed.  
The `ContextAwareExecutorService` and other propagation utilities in this library 
conform to this constraint.


  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/opentracing-span-propagation.svg
  [maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22nl.talsmasoftware.context%22%20AND%20a%3A%22opentracing-span-propagation%22
  [opentracing-img]: https://img.shields.io/badge/OpenTracing-enabled-blue.svg
  [opentracing]: http://opentracing.io
