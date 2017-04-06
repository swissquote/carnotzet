---
title: "Depending on other applications"
permalink: /creating-your-own/depending-on-applications
---
{% include toc %}

## Importing and starting other applications in your environment

When you application depends on another one, for example an SOA service, you need that other application to be started together with your application. 
Carnotzet is able to do that fairly easily, as long as your dependency has an existing Carnotzet module.

### Add a maven dependency

To import and start an application, just import it as a dependency in the pom.xml of your own application's Carnotzet.

```xml
<properties>
    <sq-wl-services-soa.version>2.4.0.sandbox1</sq-wl-services-soa.version>
</properties>
 
<dependencies>
    <dependency>
        <groupId>com.swissquote.whitelabel</groupId>
        <artifactId>sq-wl-services-soa-sandbox</artifactId>
        <version>${sq-wl-services-soa.version}</version>
    </dependency>
</dependencies>
```

### Call the dependency's crawling scripts

If your dependency includes any Oracle crawling scripts, you will need to invoke them from your application, or the final DB will not contain the necessary tables and rows for your dependency. For more details on DB crawling scripts check the Oracle Database Crawling page.

```
-- import the dependency tables.
-- You'll need to do that for both DML and DDL
-- You might need to pass parameters to your dependency's crawling script.
@sq-wl-services-soa-sandbox
```

## Rebuild and run

You can then rebuild your application's Carnotzet, and run it with the following commands

```bash
# rebuild your application's Carnotzet
mvn clean install
 
# If your dependency has crawling script, you need to crawl the database again to make sure that the tables
# and data it needs are present in the DB image
mvn zet:crawl
 
# Start your application's Carnotzet. You'll notice that dependencies also start when you run that
mvn zet:start -Dfollow
 
# You can see a summary of the health and basic information about every service running in the Carnotzet environment
# by invoking the welcome page
mvn zet:welcome
```

Controlling each application/service independently

You can start, stop, restart and monitor each service independently if needed.

```bash
# stop only the specified service
mvn zet:stop -Dservice=sq-wl-services-soa
 
# start only the specified service (silent)
mvn zet:start -Dservice=sq-wl-services-soa
 
# see the logs of the specified service
mvn zet:logs -Dservice=sq-wl-services-soa
 
# Start only the specified service (with the logs being directly shown in the console)
mvn zet:logs -Dfollow -Dservice=sq-wl-services-soa
```

### Waiting for a dependency to be up before starting an application

While this is not recommended, some applications sometimes require another service to be UP before being able to start themselves. 
Carnotzet includes a mechanism to declaratively wait on the availability of one dependency before attempting to start your application.

> This mechanism should not be used routinely. Ideally, you application should be able to tolerate the failure of one or more of its dependencies without crashing, especially at startup.
>
> This is an important aspect of resilience that your application should support, and that makes production deployments easier to manage.

### Declaring the startup dependency in your application

In order to let Carnotzet know that you application expects another service to be up before starting, you need to add a file named sandbox.properties in the src/main/resources/ folder of your application's sandbox, with the following content:
```
# Declare the name of the service that must be UP before we start
waitFor=sq-oracle-xe
 
# You can wait on multiple services using a comma separated list
waitFor=sq-oracle-xe, sq-redis
 
# Declare how long we are prepared to wait for that service to come online
# The default is 15 seconds.
waitForTimeout=500
```

### Announcing when your application is started

In order for Carnotzet to know when a service has finished initializing and can be considered UP, each Carnotzet module must announce when it is ready. 
This is done by adding a `src/main/resources/docker/scripts/container_readiness_status.sh` file to that service's own Carnotzet. This script:

1. Must return 0 when the service is UP, and any other value when the service is not UP.
1. Is automatically called every 1 second by Carnotzet

As soon as Carnotzet notices that `container_readiness_status.sh` has returned 0, all of the other services that depend on it are notified and started.

#### Example: Ready when Oracle is started

sq-oracle-xe-sandbox/src/main/resources/docker/scripts/container_readiness_status.sh

```bash
#!/bin/bash
service oracle-xe status | grep 'Instance "XE", status READY'
```

#### Example: Ready when an HTTP page responds with 200

http-service-sandbox/src/main/resources/docker/scripts/container_readiness_status.sh
```bash
#!/bin/bash
curl -s localhost:80 > /dev/null 2>&1
```

#### Example: Ready when a line is printed in the logs

```bash
#!/bin/bash
grep "rmi started" /logs/SQT/foobar.log > /dev/null 2>&1
``` 

- If you need to add some environment variables to applications read [here](/creating-your-own/configuration-using-environment-variables)
- For adding some files to dependent modules read [here](/creating-your-own/configuration-using-config-files)
