---
title: "Java API"
permalink: /user-guide/java-api
---
{% include toc %}

Introduced with version 1.1.0, the sq-modular-sandbox-core java library was extracted from the sq-modular-sandbox-maven-plugin. This allows for easy standalone usage of the sandbox in java applications and the integration of the modular sandbox with any developer tool written in Java (Tests, frameworks such as Junit4, Junit5, TestNG, IDEs such as Eclipse or IntelliJ, build tools such as Maven or Gradle.)

```xml
<dependency>
    <groupId>com.swissquote.foundation</groupId>
    <artifactId>sq-modular-sandbox-core</artifactId>
    <version>${sq-modular-sandbox.version}</version>
    <scope>test</scope>
</dependency>
```

## Creating a Carnotzet from a POM file

```java
MavenCoordinate artifact = SandboxMavenArtifact.fromPom(new File("pom.xml"));
Sandbox sandbox = new Sandbox(artifact, new MavenDependencyResolver());
```

## Creating a Carnotzet from Maven coordinates

```java
MavenCoordinate artifact = new SandboxMavenArtifact("com.swissquote","sq-artifact-id","1.3.2");
Sandbox sandbox = new Sandbox(artifact, new MavenDependencyResolver());
```

If you want to use another packaging and dependency system (NPM for example), you'd have to implement the DependencyResolver interface and instanciate that.

## Running the Carnotzet

```java
ContainerRuntime runtime = new ComposeRuntime(sandbox);
runtime.start();
```

If you want to use another container runtime (Kubernetes or Swarm for example) you'd have to implement the ContainerRuntime interface and instanciate that.
## Get services log events

```java
LogListener logEvents = new SandboxLogEvents();
runtime.registerLogListener(logEvents);
```
 

Some implementations of the Log log listeners come "out of the box" : 

- `SandboxLogEvents` : Stores receives log events in memory, allows you to wait until a certain text appears in the logs or check if some text appeared in the logs of a service (useful for test assertions).
- `StdOutLogPrinter` : Outputs the logs events to the standard output stream, log entries are prefixed by the service name (in color, using ansi escape codes if the output stream supports ansi escape codes).
- `Slf4jForwarder` : Forward log events to slf4j, the name of the logger is the name of the service.

You may use the `LogListener` interface and `LogListenerBase` abstract class to write your own log manager.

## Pluggable features

Two features have been decoupled from the Sandbox class itself and are now optional : 

* Wait for it
* Run from workspace

```java
List<SandboxFeature> features = new ArrayList<>();
features.add(new WaitForItFeature());
features.add(new DeployFromWorkspaceFeature(options, sandboxResourcesRoot, session, projectBuilder, project));
                     
sandbox = new Sandbox<>(id, new MavenDependencyResolver(), sandboxResourcesRoot, features, outputPath);
```

To implement other similar features, which modify the volumes, environment variables, entrypoints of containers, you can implement the SandboxFeature interface.
