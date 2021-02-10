[![Maven Version][maven-img]][maven] 

# ServletRequest propagation library

Adding the `servletrequest-propagation` jar to your classpath
provides static access to the current `ServletRequest`
via the `ServletRequestContextManager.currentServletRequest()` method
if the `ServletRequestContextFilter` was applied to the inbound request.

## How to use this library

1. Add it to your classpath.
  ```xml
  <dependency>
      <groupId>nl.talsmasoftware.context</groupId>
      <artifactId>servletrequest-propagation</artifactId>
      <version>[see Maven badge above]</version>
  </dependency>
  ```

2. Include the `ServletRequestContextFilter` in your application.

Done!

Now the `ServletRequestContextManager.currentServletRequest()` is propagated into each
snapshot created by the `ContextManagers.createSnapshot()` method.  
This includes all usages of the `ContextAwareExecutorService`.


  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/servletrequest-propagation
  [maven]: https://search.maven.org/artifact/nl.talsmasoftware.context/servletrequest-propagation
