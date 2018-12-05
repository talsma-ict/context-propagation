[![Released Version][maven-img]][maven] 

# Context propagation Java 8 module

Extends CompletableFuture to propagate context in a multi threaded environment.

## How to use this module

1. Add it to your classpath.
  ```xml
  <dependency>
      <groupId>nl.talsmasoftware.context</groupId>
      <artifactId>context-propagation-java8</artifactId>
      <version>[see maven-central version above]</version>
  </dependency>
  ```  
2. Example with DummyContextManager from main page:

```java
import nl.talsmasoftware.context.futures.ContextAwareCompletableFuture;

public class Main {
  public static void main(String[] args) throws InterruptedException {
    new DummyContextManager().initializeNewContext("Foo");
    ContextAwareCompletableFuture
        .runAsync(() -> {
          try {
            Thread.sleep(200);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          System.out.println(Thread.currentThread().getName()); //Common pool thread 1
          System.out.println(DummyContextManager.currentValue()); //Optional[Foo] is propagated from spawning thread
        })
        .thenCompose((aVoid) -> ContextAwareCompletableFuture.runAsync(() -> {
          System.out.println(Thread.currentThread().getName()); //Common pool thread 2
          System.out.println(DummyContextManager.currentValue());//Optional[Foo] since this thread is executed with ContextAwareExecutor
        }))
        .thenCompose((aVoid) -> {
          ContextAwareCompletableFuture<Void> ret = new ContextAwareCompletableFuture<>();
          Thread thread = new Thread(() -> {
            System.out.println(DummyContextManager.currentValue());//Optional.empty since this thread is not executed with ContextAwareExecutor
            ret.complete(null);
          });
          thread.setName("NewThread");
          thread.start();
          return ret;
        })
        .thenAccept((aVoid) -> System.out.println(DummyContextManager.currentValue()));//Optional.empty since this is NewThread

    Thread.sleep(3000);
  }
}
```

  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/context-propagation-java8.svg
  [maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22nl.talsmasoftware.context%22%20AND%20a%3A%22context-propagation-java8%22
