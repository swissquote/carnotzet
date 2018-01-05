---
title: "Managing multiple variants of configuration for the same application"
permalink: /creating-your-own/configuration-variants
---

A common use case is to have the same application with different configuration for different environments.

You should separate your configuration variants in different maven modules. 

- Specify the same `service.id` in the `carnotzet.properties` of the modules.
- You may use the same docker image specifying `docker.image` in `carnotzet.properties`.
- You may also re-use/inherit the configuration in another module by importing it as a dependency.

It has the following advantages :

 - Each variant can be re-used (imported as a maven dependency) independently
 - You can express the difference of configuration / dependencies without any duplications.

Example

Imagine we have an application named `reporting-service` for which we have two possible configuration variants : `dev` and `prod`.

The application is packaged as a docker image `my-registry.my-org/reporting-service:v2`

We would create two maven modules `reporting-service-dev-carnotzet` and `reporting-service-prod-carnotzet`, each containing different config files.

The `carnotzet.properties` of the dev variant looks like this : 
```
service.id=reporting-service
docker.image=my-registry.my-org/reporting-service:${reporting-service-dev.version}
``` 

and the prod variant : 
```
service.id=reporting-service
docker.image=my-registry.my-org/reporting-service:${reporting-service-prod.version}
```

You can also decide that the dev version depends on the prod version (by adding it as a maven dependency) to re-use prod config by default  
and add some dev-only overrides (enable debug and reduce max memory usage for example)

If multiple maven artifacts with the same service.id are present in the dependency graph, the service will be deployed only once, using the 
configuration closest to the root. If multiple variants for the same service.id are at the same level of the graph, the chosen one is not 
predictable (you can see which one was used if you enable debug logging).

Note:  the service Id should be used as folder name when defining configuration and env files, not the module's artifact Id.
The same goes for hostnames that you should use in config files.