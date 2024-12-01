[![Maven Version][maven-img]][maven] 

# SLF4J MDC propagation library

Adding the `slf4j-propagation` jar to your classpath
is all that is needed to let the [Mapped Diagnostic Context (MDC)][mdc] 
from the [Simple Logging Facade for Java (SLF4J)][slf4j] 
be automatically included into the `ContextSnapshot`.

## How to use this library

Add it to your classpath. 
```xml
<dependency>
    <groupId>nl.talsmasoftware.context</groupId>
    <artifactId>slf4j-propagation</artifactId>
    <version>[see Maven badge above]</version>
</dependency>
```

Done!

Now the `MDC.getCopyOfContextMap()` is copied into each snapshot 
from the `ContextSnapshot.capture()` method
to be reactivated by the `Contextsnapshot.reactivate()` call.
The `ContextAwareExecutorService` automatically propagates the full [MDC] content
into all executed tasks this way.


  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/slf4j-propagation
  [maven]: https://search.maven.org/artifact/nl.talsmasoftware.context/slf4j-propagation

  [slf4j]: https://www.slf4j.org/
  [mdc]: https://www.slf4j.org/api/org/slf4j/MDC.html
