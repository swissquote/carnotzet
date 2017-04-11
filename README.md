[![Build Status](https://travis-ci.org/swissquote/carnotzet.svg?branch=master)](https://travis-ci.org/swissquote/carnotzet)

# Carnotzet
Carnotzet allows you to use maven dependencies to easily share, version and re-use large modular development and testing environments.

# Features and architecture
Carnotzet is a layer of integration between maven and container orchestrators (such as docker compose) that adds the following features : 

- Dependency management : an easy way to re-use application configuration and abstract it's transitive dependencies. 
By default you don't need to understand how each dependency in your environment has to be configured and run.
- Hierarchical configuration management supporting overrides and merges.
- A java API to manage your environment in java tests (start/stop application, get IP address of a service, analyze log entries of dependencies, etc..)
- A Maven plugin to control an environment's runtime and integrate it in your build process.
- Extension points to customize environments and add cross-cutting features.

# Rationale
We like  to have lightweight, reproducible, isolated and portable development/testing environments.

This can becomes a challenge in a micro-service architecture when you start to have many shared services and middlewares (DB, JMS, Redis, etc...).

Carnotzet allows for easy re-use of applications your local environment while abstracting transitive dependencies.

With Carnotzet you don't need to mock external dependencies. We believe that mocking only delays detection of integration issues, which can create a slow feedback loop and poor developer experience.


