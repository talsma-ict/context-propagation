[![Released Version][maven-img]][maven] 

# Spring Security Context propagation library

Adding the `spring-security-context` jar to your classpath
is all that is needed to let the 
[Security Context][security context] 
from [Spring Security] 
be automatically included into the `ContextSnapshot`.

## How to use this library

Add it to your classpath. 
```xml
<dependency>
    <groupId>nl.talsmasoftware.context</groupId>
    <artifactId>spring-security-context</artifactId>
    <version>[see maven-central version above]</version>
</dependency>
```

Done!

Now the `SecurityContextHolder.getContext()` is copied into each snapshot 
from the `ContextManagers.createSnapshot()` method
to be reactivated by the `Contextsnapshot.reactivate()` call.  
The `ContextAwareExecutorService` automatically propagates the active 
[spring security] `Authentication` into all executed tasks this way.


  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/spring-security-context.svg
  [maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22nl.talsmasoftware.context%22%20AND%20a%3A%22spring-security-context%22

  [spring security]: https://projects.spring.io/spring-security/
  [security context]: https://docs.spring.io/spring-security/site/docs/current/api/index.html?org/springframework/security/core/context/SecurityContext.html
