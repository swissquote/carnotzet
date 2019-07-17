[![Build Status](https://travis-ci.org/swissquote/carnotzet.svg?branch=master)](https://travis-ci.org/swissquote/carnotzet)

# Carnotzet
Carnotzet allows you to use maven dependencies to easily share, version and re-use large modular development and testing environments.

# What are we solving ?
We like  to have lightweight, reproducible, isolated and portable development/testing environments.

This can become a challenge in a micro-service architecture when you have many shared services and middlewares (DB, JMS, Redis, etc...) managed by different teams.

Carnotzet simplifies the management of those environments, allowing you to compose/re-use existing environment definitions while abstracting transitive dependencies.

# Full documentation
Read it here : https://swissquote.github.io/carnotzet/
