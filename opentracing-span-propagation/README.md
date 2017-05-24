[![Released Version][maven-img]][maven] 
[![OpenTracing Badge][opentracing-img]][opentracing]

# OpenTracing Span propagation library

Adding the `opentracing-span-propagation` jar to your classpath
is all that is needed ... _(todo)_

## How to use this library

Add it to your classpath. 
```xml
<dependency>
    <groupId>nl.talsmasoftware.context</groupId>
    <artifactId>opentracing-span-propagation</artifactId>
    <version>[see above]</version>
</dependency>
```

Done!

Now the `GlobalTracer.getActiveSpan()` is propagated into each
background thread you start with our `ContextAwareExecutorService`.


  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/opentracing-span-propagation.svg
  [maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22nl.talsmasoftware.context%22%20AND%20a%3A%22opentracing-span-propagation%22
  [opentracing-img]: https://img.shields.io/badge/OpenTracing-enabled-blue.svg
  [opentracing]: http://opentracing.io

  [servletrequest-propagation]: servletrequest-propagation
  [opentracing-spanmanager]: https://github.com/opentracing-contrib/java-spanmanager
  [default constructor]: https://en.wikipedia.org/wiki/Nullary_constructor
