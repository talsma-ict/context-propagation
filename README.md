[![Build Status][ci-img]][ci]
[![Coverage Status][coveralls-img]][coveralls]
[![Released Version][maven-img]][maven]

# Context propagation library

Standardized context propagation in concurrent systems.

Provides a standardized way to create snapshots from various supported
`ThreadLocal`-based `Context` types that can be reactivated in another
thread.

## How to use this library

Use a `ContextAwareExecutorService` instead of your usual threadpool to start
background threads.  
It will create a snapshot of supported `ThreadLocal`-based contexts and
reactivate them in the background thread when started.
The ThreadLocal values from the calling thread will therefore automatically 
be available in the background thread as well.

## Supported contexts

The following `ThreadLocal`-based contexts are currently supported 
out of the box by this context-propagation library:

- [ServletRequest contexts][servletrequest propagation]
- [Slf4J MDC (Mapped Diagnostic Context)][mdc propagation]
- [Locale context][locale context]
- [OpenTracing Span contexts][opentracing span propagation]
- _Yours?_ Feel free to create an issue or pull-request
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
3. That's it. Now the result from your _getActiveContext_ method is propagated
   into each snapshot created by the `ContextManagers.createSnapshot()` method.
   This includes all usages of the `ContextAwareExecutorService`.

An example of a custom context implementation:
```java
public class DummyContextManager implements ContextManager<String> {
    public Context<String> initializeNewContext(String value) {
        return new DummyContext(value);
    }

    public Context<String> getActiveContext() {
        return DummyContext.current();
    }
    
    public static Optional<String> currentValue() {
        return Optional.ofNullable(DummyContext.current()).map(Context::getValue);
    }
    
    private static final class DummyContext extends AbstractThreadLocalContext<String> {
        private DummyContext(String newValue) {
            super(newValue);
        }
        
        private static Context<String> current() {
            return AbstractThreadLocalContext.current(DummyContext.class);
        }
    }
}
```

## License

[Apache 2.0 license](LICENSE)


  [ci-img]: https://img.shields.io/travis/talsma-ict/context-propagation/master.svg
  [ci]: https://travis-ci.org/talsma-ict/context-propagation
  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/context-propagation.svg
  [maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22nl.talsmasoftware.context%22%20AND%20a%3A%22context-propagation%22
  [coveralls-img]: https://coveralls.io/repos/github/talsma-ict/context-propagation/badge.svg
  [coveralls]: https://coveralls.io/github/talsma-ict/context-propagation

  [servletrequest propagation]: servletrequest-propagation
  [mdc propagation]: mdc-propagation
  [locale context]: locale-context
  [opentracing span propagation]: opentracing-span-propagation
  [default constructor]: https://en.wikipedia.org/wiki/Nullary_constructor
