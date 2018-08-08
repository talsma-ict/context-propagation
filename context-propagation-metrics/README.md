[![Maven Version][maven-img]][maven] 

# Metrics instrumentation for context-propagation

This module provides [metrics] timing instrumentation for:
 - all context snapshots that are created with `ContextManagers.createContextSnapshot()` 
 - all context snapshots that are reactivated with `ContextSnapshot.reactivate()`
 - for each specific `ContextManager`:
   - calls to `ContextManager.getActiveContext` and
   - calls to `ContextManager.initializeNewContext`
   
## How to use this module

All you need to do is add it to your classpath:
  ```xml
  <dependency>
      <groupId>nl.talsmasoftware.context</groupId>
      <artifactId>context-propagation-metrics</artifactId>
      <version>[see maven badge above]</version>
  </dependency>
  ```  

That will add `Timer` metrics to the default shared `MetricRegistry`.
For more details on the _metrics_ library, please [see its documentation][metrics].


  [maven-img]: https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/nl/talsmasoftware/context/context-propagation/maven-metadata.xml.svg
  [maven]: http://mvnrepository.com/artifact/nl.talsmasoftware.context/context-propagation-metrics
  [metrics]: https://metrics.dropwizard.io/