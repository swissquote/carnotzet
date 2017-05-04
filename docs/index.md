---
title: "Carnotzet"
excerpt: "Presenting Carnotzet"
---

The objective of Carnotzet is to create, configure and share __lightweight__, __reproducible__, __isolated__ and __portable__ development and testing environments

Modularity is a key concept in Carnotzet, you compose environments from multiple modules, developed and maintained by other teams.

* Each executable application has it's own Carnotzet module.
* Carnotzet modules are packaged with Maven and can depend directly and transitively on other Carnotzet modules.
* Applications are packaged as docker images.
* Leverages docker-compose as a lightweight container orchestrator internally.

Carnotzet is useful when your environments are becoming large with different teams managing different 
services that integrate together. If you are not in this situation, the tools is probably not adapted to your needs.

{% include toc %}

## Overview

![Overview diagram]({{ site.baseurl }}{% link _docs/carnotzet_architecture.png %})

## Why not just use docker-compose directly ?

Carnotzet is a layer on top of docker-compose that adds the following features : 

* Dependency management : an easy way to re-use configuration and abstract transitive dependencies.
* Hierarchical configuration management supporting overrides and merges.
* An easy way to integrate with tests using java code to get IP address, analyze log entries of running services, etc…


## Getting started

* Make sure you have all the [Prerequisites]({{ site.baseurl }}{% link _docs/prerequisites.md %}) on your machine.
* Checkout the repository and look at the `/examples`
* Create a Carnotzet module for your project : [User Guide - Creating a new Carnotzet module]({{ site.baseurl }}{% link _docs/creating-your-own/creating-a-new-carnotzet.md %})
* Use the [Maven plugin]({{ site.baseurl }}{% link _docs/user-guide/maven-plugin.md %}) when working on your projects
* Use the [Java API]({{ site.baseurl }}{% link _docs/user-guide/java-api.md %}) to write
Create [end-to-end tests]({{ site.baseurl }}{% link _docs/user-guide/end-to-end-tests.md %}) for your application.

## Resources

<table>
<tr><th>Issue management</th><td> https://github.com/swissquote/carnotzet/issues </td></tr>
<tr><th>Source code</th><td> https://github.com/swissquote/carnotzet </td></tr>
<tr><th>Continuous integration</th><td> https://travis-ci.org/swissquote/carnotzet</td></tr>
<tr><th>Artifacts</th><td> TODO </td></tr>
</table>