---
title: "Configuration using config files"
permalink: /creating-your-own/configuration-using-config-files
---

{% include toc %}

## Defining and injecting configuration files

Docker compose allows Carnotzet to add configuration files to a running container without adding those files to the image. 
At startup of a container, Carnotzet will automatically mount all the files placed in any subpath of the module's `src/main/resources/${service_name}/files` into the corresponding subpath from the root of the filesystem of the container.

This is best explained with an example:

Given a Carnotzet project that looks like that:

```
src/
└── main/
    └── resources/
        └── ${service_name}/
            └── files/           # the content of this folder is mounted at the root of the container
                └── softwares/
                    └── SQ/
                        └── it-config/
                            ├── sq.soa.client.something.properties
                            ├── sq.soa.client.foobar.properties
                            └── app.service.properties
```

Then the docker container started by Carnotzet will have the following files.

```
/
└── softwares/
    └── SQ/
        └── it-config/
            ├── sq.soa.client.something.properties
            ├── sq.soa.client.foobar.properties
            └── app.service.properties
```

Any kind of files can be injected in that manner. The most common use case is it-config properties.

## Overriding the configuration of dependencies

### Finding out which configuration files are supported

#### Read the docs

The list of configuration files that are supported is defined by each container or Carnotzet module, so please check the documentation of the dependency to figure out what configuration option is supported.

For most of the SQ base images and basic services, you can find the documentation in the [Docker base](/docker-base-images) images and [Configurable third party modules](/third-party-modules/) pages.

#### What to do if there is no doc…

Usually, the Carnotzet module will define a bunch of default configuration files, so you can try cloning the repository of your dependency and look for the config files defined in the Carnotzet maven module.

### Adding files in a dependency's container

You can add files into the container of any dependency in exactly the same way as you add them to the container of your own webapp. 
Just add the files in the proper subpath of `src/main/resources/${dependency_name}/files.`

```
src/
└── main/
    └── resources/
        ├── ${service_name}/
        │   └── files/
        │       └── softwares/
        │           └── SQ/
        │               └── it-config/
        │                   ├── sq.soa.client.something.properties
        │                   ├── sq.soa.client.foobar.properties
        │                   └── app.service.properties
        ├── a-dependency
        │   └── files/
        │       └── softwares/
        │           └── SQ/
        │               └── it-config/
        │                   └── sq.soa.server.tadaaa.properties
        └── b-dependency
            └── files/
                └── softwares/
                    └── SQ/
                        └── it-config/
                            └── cache.properties
```

In the example above, the container of a-dependency will contain a files at `/softwares/SQ/it-config/sq.soa.server.tadaa.properties`, and the container of b-dependency will contain a file at `/softwares/SQ/it-config/cache.properties`.
## Overriding configuration files

You can override any configuration file from any other Carnotzet module within your own environment. 
Each Carnotzet module defines its configuration files inside a `src/main/resource/${service_name}/files` folder, where `service_name` can be the name of your own service, the name of one or more dependencies, or both. 
You can therefore override config files of dependencies by putting a file with the same name and path as the file to be overriden inside the dependencies `files` folder.

Here's an example:
```
src/
└── main/
    └── resources/
        ├── my-service/               # Config files for my own Carnotzet
        │   └── files/
        │       └── haha.xml
        └── a-dependency/             # overriden ENV vars for a-dependency
            └── files/
                └── important.xml     # Paths must match the path in the original project
```

In the example above, the copy of `important.xml` included in the a-dependency Carnotzet module is completely ignored, and is instead completely replaced by the important.xml file supplied by my-service.

### Merging .properties files

Merging .properties files is similar to overriding. 
The only difference is that you need to suffix the file name to merge with .merge in your own project. 
Here is an example:

```
src/
└── main/
    └── resources/
        └── a-dependency/                    # overridden config files for a-dependency
            └── files/
                ├── config.properties.merge  # This file will be merged with the original
                └── resin.properties         # This file will just replace the original
```

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

### Merging other types of files

At the moment, the Carnotzet maven plugin only knows how to merge `.properties` files, if you need to merge other file types, you may implement the `FileMerger` SPI in the sq-sandbox-maven-plugin to add support for your file type.

### Override and merging resolution algorithm

Carnotzet resolves file overrides and merges using the standard maven dependency conflict resolution mechanism. This means that when the same file is overridden in multiple modules, the override that applies is always the one that resides in the module whose dependency path to the current module is the shortest.

See [this article](http://guntherpopp.blogspot.ch/2011/02/understanding-maven-dependency.html) for more details on the algorithm maven uses.
