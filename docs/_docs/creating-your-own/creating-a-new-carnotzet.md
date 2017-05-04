---
title: "Creating a new Carnotzet module"
permalink: /creating-your-own/creating-a-new-carnotzet
---

## Create a new maven module

The first step is to create a new maven module. Create a folder with a pom.xml

- Packaging must be "jar"
- ArtifactId should end with "-carnotzet"

Here is a minimal example :
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.github.swissquote.examples</groupId>
    <artifactId>redis-carnotzet</artifactId>
    <version>3</version>
    
    <build>
        <plugins>
            <plugin>
                <groupId>com.github.swissquote</groupId>
                <artifactId>zet-maven-plugin</artifactId>
                <version>1.0.0</version>
            </plugin>
        </plugins>
    </build>

</project>
```

let's build and run it !
```
mvn clean install zet:run
```

Which gives us :
```
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] Building redis-carnotzet latest
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ redis-carnotzet ---
[INFO] Deleting /tmp/example/target
[INFO] 
[INFO] --- maven-resources-plugin:2.6:resources (default-resources) @ redis-carnotzet ---
[WARNING] Using platform encoding (UTF-8 actually) to copy filtered resources, i.e. build is platform dependent!
[INFO] skip non existing resourceDirectory /tmp/example/src/main/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.1:compile (default-compile) @ redis-carnotzet ---
[INFO] No sources to compile
[INFO] 
[INFO] --- maven-resources-plugin:2.6:testResources (default-testResources) @ redis-carnotzet ---
[WARNING] Using platform encoding (UTF-8 actually) to copy filtered resources, i.e. build is platform dependent!
[INFO] skip non existing resourceDirectory /tmp/example/src/test/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.1:testCompile (default-testCompile) @ redis-carnotzet ---
[INFO] No sources to compile
[INFO] 
[INFO] --- maven-surefire-plugin:2.12.4:test (default-test) @ redis-carnotzet ---
[INFO] No tests to run.
[INFO] 
[INFO] --- maven-jar-plugin:2.4:jar (default-jar) @ redis-carnotzet ---
[WARNING] JAR will be empty - no content was marked for inclusion!
[INFO] Building jar: /tmp/example/target/redis-carnotzet-latest.jar
[INFO] 
[INFO] --- maven-install-plugin:2.4:install (default-install) @ redis-carnotzet ---
[INFO] Installing /tmp/example/target/redis-carnotzet-latest.jar to /home/shamu/.m2/repository/com/github/swissquote/examples/redis-carnotzet/latest/redis-carnotzet-latest.jar
[INFO] Installing /tmp/example/pom.xml to /home/shamu/.m2/repository/com/github/swissquote/examples/redis-carnotzet/latest/redis-carnotzet-latest.pom
[INFO] 
[INFO] --- zet-maven-plugin:1.0.0-SNAPSHOT:run (default-cli) @ redis-carnotzet ---
Creating network "redis_carnotzet" with driver "bridge"
Creating redis_redis_1
redis | 1:C 03 May 18:06:35.463 # Warning: no config file specified, using the default config. In order to specify a config file use redis-server /path/to/redis.conf
redis |                 _._                                                  
redis |            _.-``__ ''-._                                             
redis |       _.-``    `.  `_.  ''-._           Redis 3.2.8 (00000000/0) 64 bit
redis |   .-`` .-```.  ```\/    _.,_ ''-._                                   
redis |  (    '      ,       .-`  | `,    )     Running in standalone mode
redis |  |`-._`-...-` __...-.``-._|'` _.-'|     Port: 6379
redis |  |    `-._   `._    /     _.-'    |     PID: 1
redis |   `-._    `-._  `-./  _.-'    _.-'                                   
redis |  |`-._`-._    `-.__.-'    _.-'_.-'|                                  
redis |  |    `-._`-._        _.-'_.-'    |           http://redis.io        
redis |   `-._    `-._`-.__.-'_.-'    _.-'                                   
redis |  |`-._`-._    `-.__.-'    _.-'_.-'|                                  
redis |  |    `-._`-._        _.-'_.-'    |                                  
redis |   `-._    `-._`-.__.-'_.-'    _.-'                                   
redis |       `-._    `-.__.-'    _.-'                                       
redis |           `-._        _.-'                                           
redis |               `-.__.-'                                               
redis | 
redis | 1:M 03 May 18:06:35.465 # WARNING: The TCP backlog setting of 511 cannot be enforced because /proc/sys/net/core/somaxconn is set to the lower value of 128.
redis | 1:M 03 May 18:06:35.465 # Server started, Redis version 3.2.8
redis | 1:M 03 May 18:06:35.465 # WARNING overcommit_memory is set to 0! Background save may fail under low memory condition. To fix this issue add 'vm.overcommit_memory = 1' to /etc/sysctl.conf and then reboot or run the command 'sysctl vm.overcommit_memory=1' for this to take effect.
redis | 1:M 03 May 18:06:35.465 # WARNING you have Transparent Huge Pages (THP) support enabled in your kernel. This will create latency and memory usage issues with Redis. To fix this issue run the command 'echo never > /sys/kernel/mm/transparent_hugepage/enabled' as root, and add it to your /etc/rc.local in order to retain the setting after a reboot. Redis must be restarted after THP is disabled.
redis | 1:M 03 May 18:06:35.465 * The server is now ready to accept connections on port 6379

```

By default, each maven module corresponds to a docker application.

The name of the service/image is the part before -carnotzet in the artifactId


## carnotzet.properties

You can specify module-level configuration in `src/main/resources/carnotzet.properties`.

For example you can override the docker image :
```properties
docker.image=redis:3.0.7
```
You can create modules that only aggregate and provide configuration, but do not add any docker application to the envrionment
```properties
docker.image=none
```

## What do to next ?

So far, what we have done with the minimal example is equivalent to running `docker run redis:3`. 

That's not very interesting...

It becomes interesting when you start using [dependencies]({{ site.baseurl }}{% link _docs/creating-your-own/dependencies.md %}), 
add [configuration files]({{ site.baseurl }}{% link _docs/creating-your-own/configuration-using-config-files.md %}), 
[environment variables]({{ site.baseurl }}{% link _docs/creating-your-own/configuration-using-environment-variables.md %}) in modules.
it also allows you to easily write [end-to-end tests]({{ site.baseurl }}{% link _docs/user-guide/end-to-end-tests.md %}) for your application.
