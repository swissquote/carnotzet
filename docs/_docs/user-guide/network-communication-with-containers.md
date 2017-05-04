---
title: "Network communication with containers"
url: /user-guide/network-communication-with-containers
---

{% include toc %}

## Network topology
There is a dedicated docker network per environment runtime instance. 
All services running in the same environment can communicate with each other.

## Hostname resolution
To be able to use the same names between containers in the same environment and when using DNSDock,
The following hostname pattern should be used :
```
(container_name.)image_name.docker
```

The container_name is optional and may be used to disambiguate situations where you have the same image running 
in different environments in parallel. By default the container_name is chosen by docker-compose.

The image_name is the "short" name, without the registry and version

Example (addressing "redis" in the "app1" environment) :
```
redis.docker
# or
app1_redis.redis.docker # Disambiguates between redis instances in other environments
```

## Using a custom network aliases
You can define network aliases for a service in `src/main/resources/carnotzet.properties`:
```
network.aliases=custom-hostname
```

## How is it resolved under the hood ?
  
Depending on your environment, it may be resolved differently :

### From a developer host

You may use DnsDock. it is a DNS server running on your machine that is "container aware" and supports the recommended pattern by default.

For more information about how to install and configure DnsDock on your system, check this page : 
[https://github.com/aacebedo/dnsdock](https://github.com/aacebedo/dnsdock)

### From another docker container (ie : between 2 applications running in the same Carnotzet Environment)

We create a "user-defined" [docker bridge network](https://docs.docker.com/engine/userguide/networking/) for each Carnotzet environment, then we let docker's [embedded DNS](https://docs.docker.com/engine/userguide/networking/configure-dns/) do the resolution.

For this resolution to work, the two containers need to be "connected" to this bridge network, by default all containers managed by the Carnotzet plugin will be connected to this network.

When the Carnotzet plugin itself runs inside a docker container, it detects it and connects this container to the "user-defined" network so that communication is possible by default.

If for some reason you want communication with another container, you need to "manually" connect this other container to the Carnotzet network (see `docker network connect` command).

The embedded DNS doesn't support our pattern by default, so we generate network aliases in the docker-compose.yaml file to add the support. 

