[![Released Version][maven-img]][maven] 

# Slf4J MDC propagation library

Adding the `mdc-propagation` jar to your classpath
is all that is needed to let the [Mapped Diagnostic Context (MDC)][MDC] 
from the [Simple Logging Facade for Java (SLF4J)][Slf4J] 
be automatically included into the `ContextSnapshot`.

## How to use this library

Add it to your classpath. 
```xml
<dependency>
    <groupId>nl.talsmasoftware.context</groupId>
    <artifactId>mdc-propagation</artifactId>
    <version>[see maven-central version above]</version>
</dependency>
```

Done!

Now the `MDC.getCopyOfContextMap()` is copied into each snapshot 
from the `ContextManagers.createSnapshot()` method
to be reactivated by the `Contextsnapshot.reactivate()` call.
The `ContextAwareExecutorService` automatically propagates the full [MDC] content
into all executed tasks this way.


  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/mdc-propagation.svg
  [maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22nl.talsmasoftware.context%22%20AND%20a%3A%22mdc-propagation%22

  [slf4j]: https://www.slf4j.org/
  [mdc]: https://www.slf4j.org/api/org/slf4j/MDC.html
