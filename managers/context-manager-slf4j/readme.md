[![Maven Version][maven-img]][maven] 

# Slf4j MDC propagation library

Adding the `slf4j-propagation` jar to your classpath
is all that is needed to let the [Mapped Diagnostic Context (MDC)][mdc] 
from the [Simple Logging Facade for Java (SLF4J)][slf4j] 
be automatically included into the `ContextSnapshot`.

## How to use this library

Add it to your classpath. 
```xml
<dependency>
    <groupId>nl.talsmasoftware.context.managers</groupId>
    <artifactId>context-manager-slf4j</artifactId>
    <version>[see Maven badge above]</version>
</dependency>
```

Done!

## What does it do?

The current `MDC` map values are included in each `ContextSnapshot` when it is captured.  
Upon reactivation, these captured values (and _only_ these captured values) are reactivated in the MDC.
- MDC keys that exist in the target MDC which are _not_ part of the snapshot are left unchanged.
- MDC keys with the case-insensitive substring `"thread"`, are _not_ captured in the ContextSnashot,
  since they most-likely contain a thread-specific value.
- When a reactivation is _closed_, the _previous_ MDC values _for the captured keys_ are restored.
  All other keys that are _not_ part of the context snapshot will be left unchanged.


  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context.managers/context-manager-slf4j
  [maven]: https://search.maven.org/artifact/nl.talsmasoftware.context.managers/context-manager-slf4j

  [slf4j]: https://www.slf4j.org/
  [mdc]: https://www.slf4j.org/api/org/slf4j/MDC.html
