---
title: "Debugging"
permalink: /creating-your-own/debugging
---

## Looking at the generated docker-compose.yml
Under the hood, carnotzet generates a docker-compose v2 file and lets docker compose do the heavy lifting.

If something doesn't work like you expect, check out the generated `docker-compose.yml` file.

When using the maven plugin, this file will be in `target/carnotzet/${service-name}/docker-compose.yml` 
or in `C:\var\tmp\carnotzet_${instance_id}\docker-compose.yml` on Windows (target cannot be used because windows does not allow deleting 
files that are bind-mounted in running containers)

## Debug logging when using the java api
The java api uses SLF4J, set the log level to `DEBUG` for the `com.github.swissquote.carnotzet` package in your slf4j 
binding configuration to get more details.

## Debug logging when using the maven plugin
Run your maven command with "-X" to get detailed logging.