[![Maven Version][maven-img]][maven] 

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
    <version>[see maven badge above]</version>
</dependency>
```

Done!

Now the `MDC.getCopyOfContextMap()` is copied into each snapshot 
from the `ContextManagers.createSnapshot()` method
to be reactivated by the `Contextsnapshot.reactivate()` call.
The `ContextAwareExecutorService` automatically propagates the full [MDC] content
into all executed tasks this way.


  [maven-img]: https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/nl/talsmasoftware/context/context-propagation/maven-metadata.xml.svg
  [maven]: http://mvnrepository.com/artifact/nl.talsmasoftware.context/mdc-propagation

  [slf4j]: https://www.slf4j.org/
  [mdc]: https://www.slf4j.org/api/org/slf4j/MDC.html
