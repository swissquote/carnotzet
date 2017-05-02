---
title: "Java API"
permalink: /user-guide/java-api
---
{% include toc %}

The java library allows you to manage (start/stop, inspect, logs, etc...) carnotzet environments. It is a good way to integrate carnotzet with 
other tools and libraries (testing framework, build tools, etc...) that are interoperable with Java.

```xml
<dependency>
	<groupId>com.github.swissquote</groupId>
	<artifactId>carnotzet-core</artifactId>
	<version>${carnotzet.version}</version>
</dependency>
```
> don't forget to use test scope if you are integrating directly in test code.

### Environment identifier
We use maven coordinates (artifact id, group id, version) to uniquely identify an environment definition.

```java
MavenCoordinate env1 = CarnotzetModuleCoordinates.fromPom(Paths.get("my-carnotzet/pom.xml"));
MavenCoordinate env2 = new CarnotzetModuleCoordinates("com.example","my-artifact-id","1.3.2");
```

## Environment definition configuration
The simplest form of configuration is the following : 
```java
CarnotzetConfig config = CarnotzetConfig.builder().topLevelModuleId(env1)).build();
Carnotzet carnotzet = new Carnotzet(config);
```

## Runtime
At the moment there is only one runtime available : docker compose.
```java
ContainerOrchestrationRuntime runtime = new DockerComposeRuntime(carnotzet);
// Example usages
runtime.start();
runtime.stop("mysql");
runtime.getContainer("redis").getIp();
```

## Log management


```java
LogEvents logEvents = new LogEvents();
runtime.registerLogListener(logEvents); // can be called before or after runtime.start()
logEvents.waitForEntry("mysql", "Mysql is not ready !", 10000, 50);
		
// print logs in the test console
runtime.registerLogListener(new StdOutLogPrinter(1000, true));
```

## Get services log events
You can register log listeners which will be notified every time a log event occurs in one of the running applications. 
You can register them in the runtime at any moment (before or after services are started).

```java
LogEvents logEvents = new LogEvents();
runtime.registerLogListener(logEvents);
runtime.registerLogListener(new StdOutLogPrinter());
```
 

Some implementations are provided for convenience : 

- `LogEvents` : Stores receives log events in memory, allows you to wait until a certain text appears in the logs or check if some text appeared in the logs of a service (useful for test assertions).
- `StdOutLogPrinter` : Outputs the logs events to the standard output stream, log entries are prefixed by the service name (in color, using ansi escape codes if the output stream supports ansi escape codes).
- `Slf4jForwarder` : Forward log events to slf4j, the name of the logger is the name of the service.

You may use the `LogListener` interface and `LogListenerBase` abstract class to write your own log manager.

## Extensions

Some features are pluggable and modify the environment definition to implement cross-cutting concerns.
Extensions should implement the CarnotzetExtension interface :

```java
/**
 * An extension can modify the definition of a carnotzet environment
 * You can for example :
 *  - add/remove applications
 *  - add/remove volumes
 *  - replace entrypoint/cmd
 *  - add/remove environment variables
 */
public interface CarnotzetExtension {
	List<CarnotzetModule> apply(Carnotzet carnotzet);
}
```
They are enabled by passing them to the environment definition configuration :
```java
CarnotzetConfig config = CarnotzetConfig.builder()
	.topLevelModuleId(env1))
	.extensions(Arrays.asList(new MyExtension()))
	.build();
Carnotzet carnotzet = new Carnotzet(config);
```

