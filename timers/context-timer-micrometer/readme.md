[![Maven Version][maven-img]][maven] 

# Micrometer instrumentation for context-propagation

This module provides [Micrometer] `Timer` instrumentation for:
 - all context snapshots that are created with `ContextSnapshot.capture()` 
 - all context snapshots that are reactivated with `ContextSnapshot.reactivate()`
 - for each specific `ContextManager`:
   - calls to `ContextManager.getActiveContext` and
   - calls to `ContextManager.initializeNewContext`
   
## How to use this module

All you need to do is add it to your classpath:
  ```xml
  <dependency>
      <groupId>nl.talsmasoftware.context</groupId>
      <artifactId>context-propagation-micrometer</artifactId>
      <version>[see Maven badge above]</version>
  </dependency>
  ```  

That will add `Timer` metrics to the global composite registry
as used by the `io.micrometer.core.instrument.Metrics` utility class.
For more details on the _Micrometer_ library, please [see its documentation][micrometer].


  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/context-propagation-micrometer
  [maven]: https://search.maven.org/artifact/nl.talsmasoftware.context/context-propagation-micrometer
  [micrometer]: https://micrometer.io
