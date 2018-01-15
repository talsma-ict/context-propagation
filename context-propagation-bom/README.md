[![Released Version][maven-img]][maven] 

# Context propagation bill-of-materials

To make sure your project uses consistent versions of the 
various `context-propagation` modules, you can import this
_bill of materials_ into your maven project in a `dependencyManagement`
section of your build.

## How to import this bill-of-materials

Add the following dependency import to the `dependencyManagement`
section in your maven `pom.xml`
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>nl.talsmasoftware.context</groupId>
            <artifactId>context-propagation-bom</artifactId>
            <version>[see maven-central version above]</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

This does **not** add any dependencies to your project,
but makes sure that dependencies to any `context propagation` module
will all be of the declared version, including transitive dependencies.

  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware.context/context-propagation-bom.svg
  [maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22nl.talsmasoftware.context%22%20AND%20a%3A%22context-propagation-bom%22
