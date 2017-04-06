---
title: "Using Carnotzet for “end to end” tests"
url: /user-guide/end-to-end-tests
---

> this page is outdated and needs to be updated to reflect the changes introduced with version 1.1.0 (java api) of the sandbox3

{% include toc %} 

There is a Junit rule that you may use to start a Carnotzet before a test suite, and shut it down after the test suite.

```xml
<dependency>
    <groupId>com.swissquote.foundation</groupId>
    <artifactId>sq-modular-sandbox-junit4</artifactId>
    <version>${sandbox.version}</version>
    <scope>test</scope>
</dependency>
```

then in your test class : 

```java
@ClassRule
public static SandboxRule sandbox = new SandboxRule(new File("../sandbox/pom.xml"), true, true);
```

This will ensure that the Carnotzet environment is started and stopped properly for your test class. The last 2 parameters control the log behavior (output all logs to System.out and capture logs in memory for analysis)

## Waiting for my Carnotzet application to be ready

The Junit rule provides a utility method to wait for some test to appear in a log entry of a service :

```java
sandbox.waitForLogEntry("sq-webapp-a", "org.apache.catalina.startup.Catalina.start Server startup in", 100000, 1000);
```

Note that this only works if Carnotzet stores the logs in memory for analysis and would throw an IllegalStateException otherwise.

## Getting the IP address of a service

The Carnotzet rule can give you the IP address of any service running in the Carnotzet environment to communicate with it (this is an alternative to [User Guide - Network communication with containers](/user-guide/network-communication-with-containers))

```java
new URL("http://" + sandbox.getIpOfService("sq-my-service")+ "/sq-my-service/toto").openConnection()
```

## Assert log entries

You can perform assertions on log entries. For example if you want to assert that sq-super-service logged a line containing "Did something important" : 

```java
assertThat(sandbox.getSandboxLog().getEvents().stream()
                        .filter(event -> "sq-super-service".equals(event.getService()))
                        .map(LogEvent::getLogEntry)
                        .collect(toList()),
                hasItem(containsString("Did something important")));
```
