[![Build Status][ci-img]][ci]
[![Released Version][maven-img]][maven]

# Context propagation library

Standardized context propagation in concurrent systems.

Provides a standardized way to create snapshots from various supported
`ThreadLocal`-based `Context` types that can be reactivated in another
thread.

## How to use this library

Use any supported `ThreadLocal`-based contexts like you are used to.
Use a `ContextAwareExecutorService` instead of your usual threadpool to start
background threads, and the ThreadLocal values from the calling thread
will automatically be propagated into the background thread as well.

## Supported contexts

The following `ThreadLocal`-based contexts are currently supported 
out of the box by this context-propagation library:

- [ServletRequest contexts][servletrequest propagation]
- [OpenTracing Span contexts][opentracing span propagation]
- _yours?_ Feel free to create an issue or pull-request
  if you believe there's a general context that was forgotten. 

Adding your own `Context` type is not difficult.

## Custom contexts

It is easy to add a custom `Context` type to be propagated:

1. Implement the `ContextManager` interface.  
   Create a class with a [default constructor]
   that implements _initializeNewContext_ and _getActiveContext_ methods.
2. Create a service file called
   `/META-INF/services/nl.talsmasoftware.context.ContextManager` 
   containing the qualified class name of your `ContextManager` implementation.
3. That's it. Now the `ContextManagers.createSnapshot()` method will 
   include your context in the snapshots to be propagated.

An example of a custom context implementation:
```java
final class DummyContext extends AbstractThreadLocalContext<String> {
    DummyContext(String newValue) {
        super(newValue);
    }

    static Context<String> current() {
        return AbstractThreadLocalContext.current(DummyContext.class);
    }
}

public class DummyContextManager implements ContextManager<String> {
    public Context<String> initializeNewContext(String value) {
        return new DummyContext(value);
    }

    public Context<String> getActiveContext() {
        return DummyContext.current();
    }
}
```

  [ci-img]: https://img.shields.io/travis/talsma-ict/context-propagation/master.svg
  [ci]: https://travis-ci.org/talsma-ict/context-propagation
  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/context-propagation.svg
  [maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22nl.talsmasoftware.context%22%20AND%20a%3A%22context-propagation%22

  [servletrequest propagation]: servletrequest-propagation
  [opentracing span propagation]: opentracing-span-propagation
  [default constructor]: https://en.wikipedia.org/wiki/Nullary_constructor
