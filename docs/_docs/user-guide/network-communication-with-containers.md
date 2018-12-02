---
title: "Network communication with containers"
url: /user-guide/network-communication-with-containers
---

{% include toc %}

## Network topology
There is a dedicated docker network per environment runtime instance. 
All services running in the same instance can communicate with each other.

## Hostname resolution
To address containers running in carnotzet, you may use the following pattern :

```
(instance_id.)service_id.docker
```

- The `instance_id` is configured in the runtime (either programmatically or using `-Dinstance=...` in the maven plugin). The default value is the top level module name
- The `service_id` is the artifactId without trailing "-carnotzet" by default, it can be overridden using the `service.id` property in `carnotzet.properties`.

For example you can use the following hostnames to address the container from `redis-carnotzet` in the environment of `voting-all-carnotzet`
```
redis.docker
voting-all.redis.docker
```

The second format allows you to disambiguate when running multiple environments containing a `redis` service on the same docker host. 

## Using custom network aliases
You can add hostnames for any service using `src/main/resources/carnotzet.properties` :
```
network.aliases= db,database
```
Since version 1.2.0, you can also add aliases to your dependencies by [merging the carnotzet.properties]({{ site.baseurl }}{% link _docs/creating-your-own/configuration-using-config-files.md %}) to override the network.aliases property in other modules. To do so, create a file named `src/main/resources/my-dependency-app/carnotzet.properties.merge` with the aliases you want.

## Adding external hosts
You can add custom entries in /etc/hosts for different containers in your environment by editing the extra.hosts property in carnotzet.properties using the same syntax as docker-compose :
```
extra.hosts=ext-service1:172.16.1.3, ext-service2:172.16.1.4
```

## How is it resolved under the hood ?
  
Depending on your environment, it may be resolved differently :

### From your Linux docker host

You connect using the IP address of the container in the docker network (ie : 172.17.22.3)

You may use DnsDock to make it more robust. it is a DNS server running on your machine that is "container aware" and can reads labels on containers to provide hostname resolution.

For more information about how to install and configure DnsDock on your system, check this page : 
[https://github.com/aacebedo/dnsdock](https://github.com/aacebedo/dnsdock)

### From your MacOS or Windows machine

There are known limitations in docker for Mac and Windows that prevent communicating directly using the container ips 
(see https://docs.docker.com/docker-for-mac/networking/ and 
https://stackoverflow.com/questions/41400043/docker-on-windows-how-to-connect-to-container-from-host-using-container-ip)

As a workaround to those limitations, carnotzet automatically binds all exposed ports in containers to random (or configurable) ports 
on localhost.

You should use localhost + the mapped ports to communicate with the containers. You can use mvn zet:ps to list the mapped ports.


### From another application running in the same Carnotzet Environment

We create a "user-defined" [docker bridge network](https://docs.docker.com/engine/userguide/networking/) for each Carnotzet environment, then we let docker's [embedded DNS](https://docs.docker.com/engine/userguide/networking/configure-dns/) do the resolution.

For this resolution to work, the two containers need to be "connected" to this bridge network, by default all containers managed by the Carnotzet plugin will be connected to this network.

When the Carnotzet plugin itself runs inside a docker container, it detects it and connects this container to the "user-defined" network so that communication is possible by default.

If for some reason you want communication with another container, you need to "manually" connect this other container to the Carnotzet network (see `docker network connect` command).

We generate network aliases in the docker-compose.yaml (in case you need to debug). 

## Deprecated hostname pattern
The following pattern is deprecated but still works too : 

```
(container_name.)image_name.docker
```

The container_name is optional. By default the container_name is chosen by docker-compose.

The image_name is the "short" name, without the registry and version

Example (addressing "redis" in the "app1" environment) :
```
redis.docker
# or
app1_redis_1.redis.docker # Disambiguates between redis instances in other environments
```

If this deprecated pattern is causing conflicts in the DNS names that you use, 
you can disable this feature when you configure the environment.