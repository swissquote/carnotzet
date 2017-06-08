---
title: "Maven plugin"
url: /user-guide/maven-plugin
---

The Carnotzet maven plugin ("zet" for short) provides a simple way to manage
a carnotzet environments in your build and from the terminal.

## In your carnotzet module's pom.xml
```
<build>
	<plugins>
		<plugin>
			<groupId>com.github.swissquote</groupId>
			<artifactId>zet-maven-plugin</artifactId>
			<version>1.1.0</version>
		</plugin>
	</plugins>
</build>
```

## Example usage

```
# Start in background, but also tail the logs of the environment
> mvn zet:start -Dfollow

# Delete and re-create the container for the "redis" service running in the environment
> mvn -Dservice=redis zet:stop zet:clean zet:start

# Refresh the whole environment
> mvn zet:stop zet:clean clean install zet:start

# Check the logs of the services in the environment
> mvn zet:logs

# Open a shell prompt "into" one of the containers (for debugging purposes)
> mvn zet:shell -Dservice=redis
```

## Full refrence

```
> mvn zet:help

This plugin has 12 goals:

zet:addrs
  Lists the IP addresses of running containers

zet:clean
  Deletes stopped containers

zet:help
  Display help information on zet-maven-plugin.
  Call mvn zet:help -Ddetail=true -Dgoal=<goal-name> to display parameter
  details.

zet:logs
  Output logs, by default, logs are followed with a tail of 200
  You can override this behavior using 'follow' and 'tail' system properties
  example to get the last 5 log events from each service and return : mvn
  zet:logs -Dfollow=false -Dtail=5

zet:ps
  Lists the state of the carnotzet containers

zet:pull
  Pulls all images in the carnotzet from the docker image registry

zet:restart
  restart all services for this carnotzet if -Dservice=... is passed, ony the
  chose service will be restarted

zet:run
  Start the environment, tail the log and wait for interrupt, stops the
  environment when you interrupt with CTRL+C

zet:shell
  Starts a shell in a running container and binds it's IO to the current process

zet:start
  Start a carnotzet (in background)
  if -Dservice=... is passed, ony the chose service will be started

zet:stop
  Stop all containers
  if -Dservice=... is passed, ony the chose service will be stopped

zet:welcome
  Generate and display a welcome page

```

## Running the same environment multiple times

Each command supports a -Dinstance=... option. This allows you to start multiple instances of the same environment 
on a docker-host and control them independently. Instance names are global to the docker host, independently of the
"top level module" you are running.

```
mvn zet:start -Dinstance=myapp1
mvn zet:start -Dinstance=myapp2
mvn zet:stop -Dinstance=myapp2
mvn zet:logs -Dinstance=myapp1
```

