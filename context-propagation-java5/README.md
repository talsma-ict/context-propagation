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

Implementations are typically maintained within a static [ThreadLocal] variable.
Contexts have a very simple life-cycle: they can be created and closed.
A well-behaved Context restores the original thread-local state when it is closed.

An abstract implementation is available that takes care of random-depth nested contexts 
and restoring the 'previous' context state.

- [javadoc](https://javadoc.io/page/nl.talsmasoftware.context/context-propagation/latest/nl/talsmasoftware/context/Context.html)

# Context Manager

Manages contexts by initializing and maintaining the active context.

Normally it is not necessary to interact directly with individual context managers.
The `ContextManagers` utility class can detect available context managers and allows 
you to make a _snapshot_ of **all** active contexts all at once.

- [ContextManager javadoc][contextmanager]
- [ContextManagers javadoc][contextmanagers]

## Creating your own context manager

_(Work in progress)_

# Context Snapshot

_(Work in progress)_
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
