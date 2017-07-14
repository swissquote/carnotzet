# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [1.2.0] - 2017-07-??
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
