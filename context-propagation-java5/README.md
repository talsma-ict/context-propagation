[![Released Version][maven-img]][maven] 
[![JavaDoc pages][javadoc-img]][javadoc] 

# Context propagation

This context propagation library defines the _core concepts_ of propagating one or more
contexts.
The main use case is taking a [_snapshot_][contextsnapshot] 
of [`ThreadLocal`][threadlocal] values from the calling thread 
and _reactivating_ it in another thread.

# Context

A context can be _anything_ that needs to be maintained on the 'current thread' level.

Implementations are typically maintained with a static [ThreadLocal] variable.
Contexts have a life-cycle that is simply defined as: they can be created and closed, 
within a single thread.
A well-behaved Context restores the original value when it is closed.

An abstract implementation is available that takes care of nested contexts 
and restoring the 'previous' context state.
It contains safeguards for concurrency and out-of-sequence closing of contexts, 
although technically these use cases are not appropriate.

- [javadoc](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/Context.html)

# Context Manager

Manages contexts by initializing and maintaining an active context value.

Normally it is not necessary to interact directly with individual context managers.
The `ContextManagers` utility class detects available context managers and lets 
you take a [_snapshot_](#context-snapshot) of **all** active contexts at once.

- [ContextManager javadoc][contextmanager]
- [ContextManagers javadoc][contextmanagers]

## Creating your own context manager

_(Work in progress)_

# Context Snapshot

A context snapshot is created by the [ContextManagers]' `createContextSnapshot()` method.
The snapshot contains active context values from all known [ContextManager] implementations.
Once created, the captured _values_ in such context snapshot will not change anymore, 
even when the active context is later modified. 
The values in this snapshot can be [_reactivated_](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/ContextSnapshot.html#reactivate--) all at once in another thread. 
They stay active until the reactivation is closed again (or are overwritten by new values).  
Closing the reactivated object is mandatory (from the thread where the reactivation was called).


- [ContextSnapshot javadoc](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/ContextSnapshot.html)
- [ContextManagers javadoc](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/ContextManagers.html)


  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/context-propagation.svg
  [maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22nl.talsmasoftware.context%22%20AND%20a%3A%22context-propagation%22
  [javadoc-img]: https://www.javadoc.io/badge/nl.talsmasoftware.context/context-propagation.svg
  [javadoc]: https://www.javadoc.io/doc/nl.talsmasoftware.context/context-propagation 

  [threadlocal]: https://docs.oracle.com/javase/8/docs/api/java/lang/ThreadLocal.html
  [contextsnapshot]: https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/ContextSnapshot.html
  [contextmanager]: https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/ContextManager.html
  [contextmanagers]: https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/ContextManagers.html
