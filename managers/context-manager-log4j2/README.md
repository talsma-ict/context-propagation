[![Maven Version][maven-img]][maven] 

# Log4j 2 Thread Context propagation library

Adding the `log4j2-propagation` jar to your classpath
is all that is needed to let the [Thread Context][thread-context] 
from the [Log4j 2 library][log4j2] be automatically included into
the `ContextSnapshot`.

## How to use this library

Add it to your classpath. 
```xml
<dependency>
    <groupId>nl.talsmasoftware.context</groupId>
    <artifactId>log4j2-propagation</artifactId>
    <version>[see Maven badge above]</version>
</dependency>
```

Done!

Now the data of the Log4j 2 Thread Context is copied into each snapshot 
from the `ContextSnapshot.capture()` method
to be reactivated by the `Contextsnapshot.reactivate()` call.
The `ContextAwareExecutorService` automatically propagates the full
Thread Context data into all executed tasks this way.

When Thread Context data is propagated, it is added on top of the existing
data, if any: Thread Context stack values are pushed on top of the existing
stack; map entries are added to the existing map, only replacing existing
ones in case of a map key conflict.

Calling `ContextManager.clearAll()` will clear the Thread Context
data of the current thread.

  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/log4j2-propagation
  [maven]: https://search.maven.org/artifact/nl.talsmasoftware.context/log4j2-propagation

  [log4j2]: https://logging.apache.org/log4j/2.x/index.html
  [thread-context]: https://logging.apache.org/log4j/2.x/manual/thread-context.html
