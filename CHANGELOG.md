# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [1.7.9] - 2019-03-11
### Added
- A new maven module `carnotzet-file-merger-json` was created to support merging json configuration files.
- Added support for `replicas=n` in carnotzet.properties to scale each individual service in the environment.
### Changed
- Jackson dependency version bumped to 2.9.8
- Upgraded spotbugs dependency and configuration to support building with jdk-11

## [1.7.8] - 2018-12-02
### Added
- Docker-compose : added option to disable automatic docker network attach to the carnotzet network when running inside a container.
- DNS : added option to disable support for legacy DNS names
- Runtime : added support for "exec"
- Runtime : added support for specifying the shm_size of containers

## [1.7.7] - 2018-10-02
### Changed
- Reorder modules in generated docker-compose yml file to improve startup performance in large environments
### Fixed
- Windows compatiblity bugfix (npe)


## [1.7.6] - 2018-08-06
### Added
- Support for advanced DNS use case where current process is running inside a container using CNI
### Fixed
- Forced jersey connector to default one when fetching docker image manifests from registry to avoid clashes

## [1.7.5] - 2018-06-13
### Changed
- Added an internal cache for docker image manifests to avoid useless downloads from registry

## [1.7.4] - 2018-05-18
### Added
- Added a utility method to expose the maven dependency tree to extensions so that they can leverage the hierarchy.
### Fixed
- Fixed a bug in the selection of configuration variants, the one closer to the root module is now selected.

## [1.7.3] - 2018-03-21
### Fixed
- Fixed a bug introduced in 1.7.2 on some platform not supporting unicode characters everywhere

## [1.7.2] - 2018-03-20
### Added
- Added a utility class to simplify the creation of container startup wrapper scripts in extensions
### Fixed
- Trim output of external processes

## [1.7.1] - 2018-03-06
### Fixed
- Fixed a bug sometimes causing freezes when running external processes (eg : docker)
- Fixed fetching docker image manifests when image name contains a "/"
- Allow overriding of welcome.html fragments and ignore welcome.html files for excluded configuration variants.

## [1.7.0] - 2018-01-05
### Added
- Support for configuration variants, different maven artifacts that provide different configuration for the same service.

## [1.6.0] - 2018-01-03
### Added
- Local ports of the docker host can now be bound to container ports. All ports are mapped (to random available ports)
 by default on Windows and MacOS since there is no docker0 bridge in those environments
### Fixed
- Fixed Windows and MacOS compatibility bugs
- Fixed a bug in PullPolicy.IF_LOCAL_IMAGE_ABSENT where the image would not be pulled even when it was not present locally
### Changed
- Added exceptions when invocation of external commands (docker / docker-compose) return non-zero exit codes
- It is now possible to pull images from extensions (without requiring a reference to the container orchestration runtime)

## [1.5.4] - 2017-12-13
### Fixed
- No longer ignore -Dservice=... in zet:clean goal of the maven plugin
- Fixed Windows compatibility issue
- Fixed Error message describing detected cycles
- Fixed an error when downloading the top level pom file from a remote maven repo (the bug was introduced in 1.5.0)

## [1.5.3] - 2017-12-07
### Changed
- Added exit-code check on internal maven invocations
- Added check for uniqueness of artifactIds in environments
- Improved error message when dependency cycles are detected
### Fixed
- Windows compatibility fixes
## [1.5.2] - 2017-12-07
### fixed
- Fix IOException when merging into non-existing file
## [1.5.1] - 2017-12-07
### Changed
- Add option to degrade the correctness of configuration overrides in cases of dependency cycles instead of failing. The default behavior remains unchanged (fail).
- Configuration in .merge files is not ignored when there is no target file to merge with.
- Add support for `start.by.default=false` in carnotzet.properties to support optional services.
## [1.5.0] - 2017-11-18
### Added
- Classifiers can now be used instead of suffixes in the artifactId to define carnotzet maven artifacts
### Fixed
- Use resources in jar files in all cases (even when top level module resources path is provided)
## [1.4.0] - 2017-11-03
### Added
- Support for image pulling policies in docker-compose runtime
## [1.3.0] - 2017-10-23
### Added
- Support for maven version placeholders in docker.image versions. Example : docker.image=my-image:${my-module.version}
- Support for extra entries in /etc/hosts in containers, use the extra.hosts property in carnotzet.properties
### Fixed
- Ignore MAVEN_DEBUG_OPTS when invoking maven to resolve dependencies, this allows to debug (with suspend=y) 
without the sub-maven process also suspending.
- Custom network aliases are now properly exported as dnsdock aliases.
- Improved the error message in case of cycles in the maven dependency graph
## [1.2.3] - 2017-09-21
### Fixed
- Detect cycles in maven dependency graphs to avoid StackOverflowErrors in case of cycles.
### Changed
- Ignore all artifacts imported with scopes other than compile and runtime.
- Allow injection/override of arbitrary files and folders in module resources by default.
## [1.2.2] - 2017-08-25
### Fixed
- Fixed a bug where configuration file overrides were not applied properly in some cases
### Changed
- Changed maven resolver from shrinkwrap to maven invoker + maven-dependency-plugin
- The core library now depends on having a functional maven installation in the environment (M2_HOME or ${maven.home} must be set)
## [1.2.1] - 2017-08-03
### Fixed
- Fixed "welcome" page generation regression introduced in 1.2.0
## [1.2.0] - 2017-07-17
### Added
- Possibility to configure docker.entrypoint and docker.cmd in carnotzet.properties
- Possibility to override cartnozet.properties of downstream dependencies.
### Changed
- Removed support for pattern `{service_name}.network.aliases=...` in carnotzet.properties. Use the new carnotzet.properties override/merge feature instead with the `network.aliases` key.
### Fixed
- Maven dependencies are now better aligned. Fixes issues with aether-utils backwards compatibility experienced by some uers.. Minimal compatible version of Maven is now 3.2.5 
### Internal
- Refactored internal resource file structure to keep both expanded-jars and resolved resources to simplify testing and debugging
- Added maven wrapper to fix maven version issues in travis-ci
## [1.1.0] - 2017-06-09
### Changed
- Hostname pattern to establish network connections with containers changed from `(container_name.)image_name.docker` to `(instance_id.)module_name.docker`
### Added
- Possibility to configure extensions in pom.xml files
### Fixed
- Prevent NPE when programmatically adding modules that have no labels or properties
- e2e tests randomly freezing
## [1.0.0] - 2017-05-04

Intitial release. 
