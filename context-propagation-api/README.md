[![Released Version][maven-img]][maven] 
[![JavaDoc pages][javadoc-img]][javadoc] 

# Context propagation API

The `context-propagation-api` module implements the _core concepts_ 
of propagating contexts.  
The main use case is _capturing_ a [snapshot][contextsnapshot] 
of [`ThreadLocal`][threadlocal] values from the calling thread 
and _reactivating_ it in another thread.

## Key concepts

The terms [Context Snapshot](#context-snapshot), [Context](#context)
and [Context Manager](#context-manager) are crucial to understanding this library.

### Context Snapshot

A new context snapshot is captured by the [ContextSnapshot] `capture()` method.  
Each snapshot contains active context values from all known [ContextManager] implementations.  
The values in this snapshot can be [_reactivated_](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/ContextSnapshot.html#reactivate--) all at once in another thread. 
They stay active until the reactivation is closed again (or are overwritten by new values).  
Closing the reactivated object is mandatory (from the thread where the reactivation was called).

Once created, the captured _values_ in a context _snapshot_ do not change anymore, 
even when the active context is later modified.
Although reactivating a single snapshot multiple times in different threads is possible,
the 'normal' use case is capturing a new snapshot from a parent thread and reactivating it in a background thread.

- [ContextSnapshot javadoc](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/ContextSnapshot.html)

### Context Manager

Manages contexts by initializing and maintaining an active context value.

Normally it is not necessary to interact directly with individual context managers.
The api detects available context managers and lets 
you capture a [_snapshot_](#context-snapshot) of **all** active contexts at once.

- [ContextManager javadoc][contextmanager]
- [ContextSnapshot javadoc][contextsnapshot]

### Context

A context can be _anything_ that needs to be maintained on the 'current thread' level.

Implementations are typically maintained with a static [ThreadLocal] variable.
Contexts have a life-cycle that is simply defined as: they can be created and closed, 
within a single thread.
A well-behaved Context restores the original value when it is closed.

An abstract implementation is available that takes care of nested contexts 
and restoring the 'previous' context state.
It contains safeguards for concurrency and out-of-sequence closing of contexts, 
although technically these use cases are not appropriate.

- [Context javadoc][context]

## Creating your own context manager

1. Create a context manager.  
Implement the  `nl.talsmasoftware.context.ContextManager` interface.  
Make sure your class has a public [default constructor](https://en.wikipedia.org/wiki/Nullary_constructor).
  
2. Register your context manager.  
Add a service file to your application called `/META-INF/services/nl.talsmasoftware.context.ContextManager`.  
It should contain the fully qualified classname of your implementation.

### Example context manager implementation

```java
public class DummyContextManager implements ContextManager<String> {
  public Context<String> initializeNewContext(String value) {
    return new DummyContext(value);
  }

  public String getActiveContextValue() {
    return DummyContext.current().map(DummyContext::getValue).orElse(null);
  }
  
  private static final class DummyContext extends AbstractThreadLocalContext<String> {
    private DummyContext(String newValue) {
      super(newValue);
    }

    private static Optional<Context<String>> current() {
      return Optional.ofNullable(AbstractThreadLocalContext.current(DummyContext.class));
    }
  }
}
```


  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/context-propagation.svg
  [maven]: https://search.maven.org/artifact/nl.talsmasoftware.context/context-propagation
  [javadoc-img]: https://www.javadoc.io/badge/nl.talsmasoftware.context/context-propagation.svg
  [javadoc]: https://www.javadoc.io/doc/nl.talsmasoftware.context/context-propagation 

  [threadlocal]: https://docs.oracle.com/javase/8/docs/api/java/lang/ThreadLocal.html
  [context]: https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/api/Context.html
  [contextsnapshot]: https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/api/ContextSnapshot.html
  [contextmanager]: https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/api/ContextManager.html
