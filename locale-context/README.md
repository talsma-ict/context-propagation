[![Maven Version][maven-img]][maven] 

# Locale context module

This module allows an application to maintain a custom `Locale`
in a context that is bound to the current thread,
allowing a configured `Locale` to be propagated.  

## How to use this module

1. Add it to your classpath.
  ```xml
  <dependency>
      <groupId>nl.talsmasoftware.context</groupId>
      <artifactId>locale-context</artifactId>
      <version>[see Maven badge above]</version>
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
  private void someMethod() {
      Locale locale = LocaleContextManager.getCurrentLocaleOrDefault();
  } 
  ```


  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/locale-context
  [maven]: https://search.maven.org/artifact/nl.talsmasoftware.context/locale-context
