---
title: "Configuration of applications using files"
permalink: /creating-your-own/configuration-using-config-files
---

{% include toc %}

## How are configuration files managed ?

To enable easy re-use of configuration files, they are packaged in the jar file of the carnotzet module.
When you start an environment, those files will be mounted into corresponding application containers.

You can define configuration files to be mounted in the containers of any downstream dependency (direct or indirect). 
This can be used to override/merge the configuration of applications in your environment.

## Directory Structure in the carnotzet module

From the root of the resources of the module's Jar : 

- The first directory defines the application in which the files will be mounted (can be itself, or a dependency).
- The second level of directory is "files" by convention (to distinguish with other ways to configure applications
 such as environment variables)
- Sub-directories in "files" define the target location of the file in the application's container
 (think of the "files" folder as "/")

This is best explained with an example, let's imagine we have a module named "my-app" which has it's own configuration 
file (expected to be /conf/application.properties in the code of my-app).
Now the environment also depends on an nginx instance with custom configuration.

It would look like this in my-app's module:

```
src/
└── main/
    └── resources/
        ├── my-app/
        │   └── files/    # the content of this folder is mounted at the root of the container
        │       └── config/
        │           └── application.properties
        └── nginx/
            └── files/    # the content of this folder is mounted at the root of the container
                └── etc/
                    └── nginx/
                        └── conf/
                            └── nginx.conf
                 
```

Any kind of files can be packaged in the maven jar and mounted in applications like this.

## Overriding and merging files

You can override any configuration file from any other Carnotzet module within your own environment. 
Each Carnotzet module defines its configuration files inside a `src/main/resource/${service_name}/files` folder, where `service_name` can be the name of your own service, the name of one or more dependencies, or both. 
You can therefore override config files of dependencies by putting a file with the same name and path as the file to be overriden inside the dependencies `files` folder.

To merge, the only difference is that you need to suffix the file name to merge with .merge in your own project. 

Here's an example:

in "a-dependency" module 
```
src/
└── main/
    └── resources/
        └── a-dependency/
            └── files/
                └── config.properties
                └── important.xml
```
in "my-service" module (which depends on "a-dependency")
```
src/
└── main/
    └── resources/
        ├── my-service/
        │   └── files/
        │       └── haha.xml
        └── a-dependency/             
            └── files/
                ├── config.properties.merge # Merge
                └── important.xml           # Override
```

In the example above, the copy of `important.xml` included in the a-dependency Carnotzet module is completely ignored, and is instead completely replaced by the important.xml file supplied by my-service.

When properties files are merged, the resulting file contains ALL of the properties that are defined in at least one file, and will keep only one value for each variable. The final value for each variable is selected using the maven dependency conflict resolution algorithm (see below).

Example:
```
config.properties         config.properties.merge         result
=================    +    =======================    =    ======
a.b=ab                    a.b=123456                      a.b=123456
c.d=cd                    foo=foo                         c.d=cd
e.f=ef                                                    e.f=ef
                                                          foo=foo
```                                                         

### Merging/Overriding carnotzet.properties
In some cases, you may want to override the cartnozet configuration of a module (docker.image, network.aliases, etc...)
It can be done by creating a file `src/main/resources/{a-dependency}/carnotzet.properties(.merge)` in your module. This feature was added in version 1.2.0

### Merging other types of files

At the moment, Carnotzet only knows how to merge `.properties` files, if you need to merge other file types, you may implement the `FileMerger` SPI to add support for your file type.

### Override and merging resolution algorithm

Carnotzet resolves file overrides and merges using the standard maven dependency conflict resolution mechanism. This means that when the same file is overridden in multiple modules, the override that applies is always the one that resides in the module whose dependency path to the current module is the shortest.

See [this article](http://guntherpopp.blogspot.ch/2011/02/understanding-maven-dependency.html) for more details on the algorithm maven uses.
