---
title: "Managing multiple variants of configuration for the same application"
permalink: /creating-your-own/configuration-variants
---

A very common use case is to have the same application with different configuration for different environments.

You might be tempted to use maven profiles to express those variants. Don't ! maven profiles do not allow your upstream dependencies to re-use 
configuration and dependencies.

Instead you should separate your configuration variants in different maven modules. 

You can leverage the following features to do it :

- You can use the same docker image in different carnotzet modules by specifying docker.image in carnotzet.properties.
- You can also re-use the configuration in another module by importing it as a dependency.


It has the following advantages :

 - Each variant can be re-used (imported as a maven dependency) independently
 - You can express the difference of configuration / dependencies without any duplications.

