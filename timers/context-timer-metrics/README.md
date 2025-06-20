[![Maven Version][maven-img]][maven]

# Metrics instrumentation for context-propagation

This module provides [metrics] `Timer` instrumentation for:

- all context snapshots that are created with `ContextSnapshot.capture()`
- all context snapshots that are reactivated with `ContextSnapshot.reactivate()`
- for each specific `ContextManager`:
    - calls to `ContextManager.activate` and
    - calls to `ContextManager.getActiveContextValue`

## How to use this module

All you need to do is add it to your classpath:

  ```xml
  <dependency>
      <groupId>nl.talsmasoftware.context</groupId>
      <artifactId>context-propagation-metrics</artifactId>
      <version>[see Maven badge above]</version>
  </dependency>
  ```  

That will add `Timer` metrics to the default shared `MetricRegistry`.
For more details on the _metrics_ library, please [see its documentation][metrics].


  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/context-propagation-metrics
  [maven]: https://search.maven.org/artifact/nl.talsmasoftware.context/context-propagation-metrics
  [metrics]: https://metrics.dropwizard.io/
