---
title: "carnotzet.properties configuration reference"
permalink: /creating-your-own/properties-reference
---

Here are the main properties that are supported by default in `src/main/resources/carnotzet.properties`. 
Extensions may add their own properties.

You can override those properties for any of your dependencies with `src/main/resources/{my-dependency}/carnotzet.properties`
 or `src/main/resources/{my-dependency}/carnotzet.properties.merge` 

docker.image
============
Example : `docker.my.org/my-app:3.4`

You can re-use maven module versions using placeholders, for example : `docker.my.org/my-app:${my-app.version}`

The default registry is `docker.io`, this can be configured globally with CarnotzetConfiguration.

The default version is the carnotzet maven module's version (pom.xml)

The default image name is the carnotzet maven module's artifact id without the `-carnotzet` suffix

docker.entrypoint
=================
Override the entrypoint of the docker image

docker.cmd
==========
Override the command of the docker image

start.by.default
================
Indicates if the service should be started or not when running the global start command.
This is useful to manage optional services in the environment.