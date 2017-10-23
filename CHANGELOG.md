# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [1.2.4] - 2017-10-23
### Added
- Support for maven version placeholders in docker.image versions. Example : docker.image=my-image:${my-module.version}
- Support for extra entries in /etc/hosts in containers, use the extra.hosts property in carnotzet.properties
### Fixed
- Ignore MAVEN_DEBUG_OPTS when invoking maven to resolve dependencies, this allows to debug (with suspend=y) 
without the sub-maven also suspending.
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
