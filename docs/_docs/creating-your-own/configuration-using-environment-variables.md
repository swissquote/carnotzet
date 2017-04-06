---
title: "Configuration using environment variables"
permalink: /creating-your-own/configuration-using-environment-variables
---

{% include toc %}

## Defining environment variables

Docker supports passing an arbitrary number of environment variables to running containers. 
These environment variables are the basic UNIX shell environment variables (eg: `JAVA_HOME`, `PATH`, etc…). 
Your Carnotzet module is free to define and use as many environment variables as necessary.

The values for these environment variables must be placed in `.env` files, in the `src/main/resources/SERVICE_NAME/env` folder, where `SERVICE_NAME` must be replaced by the name of your service. 
You may place as many `.env` files as needed in that folder, all of them will be taken into account.

```
src/
└── main/
    └── resources/
        └── ${service_name}/
            └── env/
                ├── something.env
                ├── foo.env
                └── bar.env
```

.env files use the following format
```
# this is the standard .env file format
# one variable per line, no spaces.
# comments start with #, blank lines are ignored.
VARIABLE1=value1
VARIABLE2=value2
```

Avoid defining the same environment variable in multiple files. If you do so, only one of the values will be taken into account, but it can be hard to predict which one it will be.

Note that your Carnotzet module can override the environment variables of other Carnotzet modules by adding .env files to the `src/main/resources/DEPENDENCY_SERVICE_NAME/env` folder.

When you make changes to any of the .env files, you must rebuild your Carnotzet module and restart the container whose configuration has changed.

```bash
# rebuild the Carnotzet module
cd ${project_home}/sandbox;
mvn clean install
 
# restart the container
mvn zet:restart -Dservice=${service_name}
```

You can find more informations about the usage of environment variables in docker at:

* [Environment variables with Docker](http://serverascode.com/2014/05/29/environment-variables-with-docker.html) 
* [Environment variables in Compose](https://docs.docker.com/compose/environment-variables/)

## Overriding the configuration of dependencies

### Finding out which environment variables are supported

#### Read the docs
The list of environment variables that are supported is defined by each container or Carnotzet module, so please check the documentation of the dependency to figure out what configuration option is supported.

For most of the SQ base images and basic services, you can find the documentation in the [Docker base](/docker-base-images) images and [Configurable third party modules](/third-party-modules/) pages.

#### What to do if there is no doc…

Sometimes, the documentation of a dependency is missing or incomplete. You can do the following thing to have an idea of what variables can be configured.

```bash
# run your projects own Carnotzet environment, making sure that the dependency you want to inspect is started
mvn zet:start
 
# Open a bash shell on the container you want to inspect
mvn zet:shell
 
# In the dependency shell, print out all the environment variables
env
```

## Overriding environment files

You can override any environment file from any other Carnotzet module within your own environment. 
Each Carnotzet module defines its environment files inside a src/main/resource/${service_name}/env folder, where service_name can be the name of your own service, the name of one or more dependencies, or both. You can therefore override .env files of dependencies by putting a file with the same name as the file to be overriden inside the dependencie's env folder.

Here's an example:
```
src/
└── main/
    └── resources/
        ├── my-service/               # ENV vars for my own Carnotzet
        │   └── env/
        │       ├── my-service.env
        │       └── tomcat.env
        └── a-dependency/             # overriden ENV vars for a-dependency
            └── env/
                ├── a-dependency.env  # env file names must match .env files
                └── tomcat.env        # defined in the a-dependency project
```

In the example above, the copy of a-dependency.env included in the a-dependency Carnotzet module is completely ignored, and is instead completely replaced by the a-dependency.env file supplied by my-service.

### Merging environment files

Merging environment files is similar to overriding. The only difference is that you need to suffix the file name to merge with .merge in your own project. Here is an example:

```
src/
└── main/
    └── resources/
        ├── my-service/                     # ENV vars for my own Carnotzet
        │   └── env/
        │       ├── my-service.env
        │       └── tomcat.env
        └── a-dependency/                   # overriden ENV vars for a-dependency
            └── env/
                ├── a-dependency.env.merge  # This file will be merged with the original
                └── tomcat.env              # This file will just replace the original
```
When ENV files are merged, the resulting file contains ALL of the environment variables that are defined in at least one file, and will keep only one value for each variable. The final value for each variable is selected using the maven dependency conflict resolution algorithm (see below).

Example:
```
a-dependency.env         a.dependency.env.merge         result
================    +    ======================    =    ======
A=a                      B=123456                       A=a
B=b                      C=c                            B=123456
                                                        C=c
```

### Override and merging resolution algorithm

Carnotzet resolves file overrides and merges using the standard maven dependency conflict resolution mechanism. This means that when the same file is overridden in multiple modules, the override that applies is always the one that resides in the module whose dependency path to the current module is the shortest.

See [this article](http://guntherpopp.blogspot.ch/2011/02/understanding-maven-dependency.html) for more details on the algorithm maven uses.
