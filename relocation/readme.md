# Relocation

This directory contains [Maven relocation](https://maven.apache.org/guides/mini/guide-relocation.html) POMs
for modules that were once published in Maven Central, 
but whose _artifactId_ have either been renamed or moved to another _groupId_.

The published _relocation_ POM files will serve as a redirect for dependent
projects to the new location of these resources.

## Overview

The following relocations are published for this library:
- old-root: Moves `nl.talsmasoftware:context-propagation-root` to groupId `nl.talsmasoftware.context`.
- java5: Moves `nl.talsmasoftware:context-propagation` to groupId `nl.talsmasoftware.context`.
- java8: Moves `nl.talsmasoftware:context-propagation-java8` to groupId `nl.talsmasoftware.context`.
- mdc: Renames `nl.talsmasoftware.context:mdc-propagation` to `nl.talsmasoftware.context:slf4j-propagation`.
