---
title: "Carnotzet"
excerpt: "Presenting Carnotzet"
---

The objective of Carnotzet is to be able to create, configure and share __lightweight__, __reproducible__, __isolated__ and __portable__ development and testing environments

Modularity is a key concept in Carnotzet as you can create an environment from multiple modules, developed and maintained by other teams.


* Each executable application has it's own Carnotzet module.
* Carnotzet modules are packaged with Maven and can depend directly and transitively on other Carnotzet modules.
* Each Carnotzet leverages Docker and Dockerfiles to package applications in their own image.
* Leverages docker-compose as a lightweight container orchestrator internally.

{% include toc %}

## Resources

<table>
<tr><th>Issue management</th><td> https://github.com/swissquote/carnotzet/issues </td></tr>
<tr><th>Source code</th><td> https://github.com/swissquote/carnotzet </td></tr>
<tr><th>Continuous integration</th><td> TODO </td></tr>
<tr><th>Code quality</th><td> TODO </td></tr>
<tr><th>Artifacts</th><td> TODO </td></tr>
</table>

## Overview

![Overview diagram]({{ site.baseurl }}{% link _docs/carnotzet_architecture.png %})

## Why not just use docker-compose directly ?

Carnotzet is a layer on top of docker-compose that provides the following features : 

* Dependency management : an easy way to re-use configuration and abstract transitive dependencies.
* Hierarchical configuration management supporting overrides and merges.
* A modular database crawler to extract a coherent datasets for a whole project by leveraging existing carnotzet modules.
* An easy and standard way to tell an application to wait before another is ready before starting
* An easy way to integrate with tests using java code to get IP address, analyze log entries of running services, etc…
* Automatic detection and deployment of workspace libraries and applications into containers for a fast feedback loop in development mode

If you don't need any of those features, please remember that re-usability and the DB crawler configuration may actually help other teams to integrate with your applications in their development environment.
Getting started

* Make sure you have all the [Prerequisites]({{ site.baseurl }}{% link _docs/prerequisites.md %}) on your machine
* Checkout the repository and look in `/examples`
* Create a Carnotzet module for your project : [User Guide - Sandboxing your application]({{ site.baseurl }}{% link _docs/creating-your-own/sandboxing-your-application.md %})


## Commands / help


```bash

This plugin has 12 goals:
  
#  Start one (if you use -Dservice=…) or all containers for this Carnotzet environment in background. The containers will continue running after the maven process is done. As a convenience, you may use -Dfollow to tail the logs directly (equivalent to "mvn zet:start zet:logs")
zet:start
 
#  Starts one (if you use -Dservice=…) or all containers for this Carnotzet environment in foreground. The containers will be stopped when you interrupt the maven process.
zet:run
 
#  Generate and display a welcome page providing useful information about running services.
zet:welcome
 
#  Output logs, by default, logs are followed with a tail of 200
#  You can override this behavior using 'follow' and 'tail' system properties
#  example to get the last 5 log events from each service and return : mvn
#  zet:logs -Dfollow=false -Dtail=5
zet:logs
 
# Starts a shell in a running container and binds it to the current terminal
zet:shell
 
# Restart one (if you use -Dservice=…) or all containers for this Carnotzet environment
zet:restart
 
# Stops one (if you use -Dservice=…) or all containers for this Carnotzet environment
zet:stop
 
# Deletes stopped containers
zet:clean
 
# Generates a data image for this Carnotzet.
zet:crawl
 
#  Lists the IP addresses of running containers (you should not need it and use "User-guide - Network communication with containers" instead)
zet:addrs
 
#  Lists the state of the Carnotzet containers
zet:ps
  
#  Display help information on sq-modular-sandbox-maven-plugin.
#  Call mvn zet:help -Ddetail=true -Dgoal=<goal-name> to display parameter
#  details.
zet:help

```
