[![Released Version][maven-img]][maven] 
[![JavaDoc pages][javadoc-img]][javadoc] 

# Context propagation

The main `context-propagation` module implements the _core concepts_ 
of propagating contexts.  
The main use case is taking a [_snapshot_][contextsnapshot] 
of [`ThreadLocal`][threadlocal] values from the calling thread 
and _reactivating_ it in another thread.

## Key concepts

The terms [Context](#context), [Context Manager](#context-manager) 
and [Context Snapshot](#context-snapshot) are crucial to understanding this library.

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

### Context Manager

Manages contexts by initializing and maintaining an active context value.

Normally it is not necessary to interact directly with individual context managers.
The api detects available context managers and lets 
you capture a [_snapshot_](#context-snapshot) of **all** active contexts at once.

- [ContextManager javadoc][contextmanager]
- [ContextSnapshot javadoc][contextsnapshot]

### Context Snapshot

A context snapshot is captured by the [ContextSnapshot]' `capture()` method.
The snapshot contains active context values from all known [ContextManager] implementations.
Once created, the captured _values_ in such context snapshot will not change anymore, 
even when the active context is later modified. 
The values in this snapshot can be [_reactivated_](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/ContextSnapshot.html#reactivate--) all at once in another thread. 
They stay active until the reactivation is closed again (or are overwritten by new values).  
Closing the reactivated object is mandatory (from the thread where the reactivation was called).

- [ContextSnapshot javadoc](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/ContextSnapshot.html)

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

  public Context<String> getActiveContextValue() {
    DummyContext current = DummyContext.current();
    return current != null ? current.getValue() : null;
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


  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/context-propagation.svg
  [maven]: https://search.maven.org/artifact/nl.talsmasoftware.context/context-propagation
  [javadoc-img]: https://www.javadoc.io/badge/nl.talsmasoftware.context/context-propagation.svg
  [javadoc]: https://www.javadoc.io/doc/nl.talsmasoftware.context/context-propagation 

  [threadlocal]: https://docs.oracle.com/javase/8/docs/api/java/lang/ThreadLocal.html
  [context]: https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/api/Context.html
  [contextsnapshot]: https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/api/ContextSnapshot.html
  [contextmanager]: https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/api/ContextManager.html
