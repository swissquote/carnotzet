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
Example : `docker.image=docker.my.org/my-app:3.4`

You can re-use maven module versions using placeholders, for example : `docker.my.org/my-app:${my-app.version}`

The default registry is `docker.io`, this can be configured globally with CarnotzetConfiguration.

The default version is the carnotzet maven module's version (pom.xml)

The default image name is the carnotzet maven module's artifact id without the `-carnotzet` suffix

You can specify `none` as a value to indicate that this module only provides configuration and dependencies, 
but doesn't add any service to the environment.

docker.entrypoint
=================
Override the entrypoint of the docker image, has to be a JSON array.

example : `docker.entrypoint = ["executable", "param1", "param2"]`

docker.cmd
==========
Override the command of the docker image, has to be a JSON array.

docker.shm_size
===============
The size of the /dev/shm in the container.

examples :
- `docker.cmd = ["executable", "param1", "param2"]` (exec form, this is the preferred form)
- `docker.cmd = ["param1","param2"]` (as default parameters to entrypoint)

start.by.default
================
Indicates if the service should be started or not when running the global start command.
This is useful to manage optional services in the environment.

exposed.ports
=============
Map ports of localhost to container ports.

The format to use is the same as in docker-compose : https://docs.docker.com/compose/compose-file/compose-file-v2/#ports

Multiple mappings can be specified (coma separated)

example : `exposed.ports = 8080:80,443:443`

Be careful when manually mapping ports like this as you need to manage conflicts. Note that you can hierarchically override those mappings in 
your dependencies to resolve conflicts using different ports

This property is only taken into account if the runtime is configured to map localhost ports. This is the case by default on MacOS and 
Windows but not on Linux. The reason is that only Linux allows communicating directly with containers using their IP addresses.

When using the maven plugin, you can choose if localhost ports should be bound or not using `-DbindLocalPorts=(true|false)`

service.id
============
The unique ID of the service in the environment. This is useful when you have mutually exclusive configuration variants of the same application in different 
maven artifacts. See [configuration variants]({{ site.baseurl }}{% link _docs/creating-your-own/configuration-variants.md %}) for details and examples.

Default : the first capture group of the moduleFilterPattern in CarnotzetConfig. The default pattern is `(.*)-carnotzet`. Example : artifactID `redis-carnotzet` -> `redis`

Note: This property cannot be overridden hierarchically because it determines the folder names that must be used to override configuration.
