[![Released Version][maven-img]][maven] 

# Locale context module

This module allows an application to maintain a custom `Locale`
in a context that is bound to the current thread,
allowing a configured `Locale` to be propagated.  

Adding the `servletrequest-propagation` jar to your classpath
provides static access to the current `ServletRequest`
via the `ServletRequestContextManager.currentServletRequest()` method
if the `ServletRequestContextFilter` was applied to the inbound request.

## How to use this module

1. Add it to your classpath.
  ```xml
  <dependency>
      <groupId>nl.talsmasoftware.context</groupId>
      <artifactId>locale-context</artifactId>
      <version>[see maven-central version above]</version>
  </dependency>
  ```  
2. Make sure to use the `ContextAwareExecutorService` as your threadpool.
3. Set the current Locale for some block of code:
   ```java
   private static LocaleContextManager localeContextManager = new LocaleContextManager();

   private void runWithLocale(Locale locale, Runnable someCode) {
       try (Context<Locale> ctx = localeContextManager.initializeNewContext(locale)) {
           someCode.run();
       }
   } 
   ```
4. Use the LocaleContext anywhere in your application:
  ```java
  import nl.talsmasoftware.context.locale.LocaleContextManager;
  private void someMethod() {
      Locale locale = Optional.ofNullable(LocaleContextManager.getCurrentLocale()).orElseGet(Locale::getDefault);
  } 
  ```


  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/locale-context.svg
  [maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22nl.talsmasoftware.context%22%20AND%20a%3A%22locale-context%22
