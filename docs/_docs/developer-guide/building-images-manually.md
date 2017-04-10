---
title: "Building all images manually"
permalink: /developer-guide/building-images-manually
---

Clone sq-soa-platform code

```bash
mkdir -p /work/foundation
cd /work/foundation
hg clone https://mercurial.bank.swissquote.ch/hg/foundation/sq-soa-platform
```

Build the base image for soa applications running with java8 and tomcat8

```bash
export DOCKER_INTERFACE_ADDRESS=$(ip -f inet -o addr show docker0|cut -d ' ' -f 7 | cut -d '/' -f 1)
docker build -t sq_soa_tomcat --build-arg http_proxy="http://$DOCKER_INTERFACE_ADDRESS:3129" /work/foundation/sq-soa-platform/service-jersey/docker/java8tomcat8
```

Clone the POC project

```bash
mkdir -p /work/foundation/sq-modular-sandbox/
cd /work/foundation/sq-modular-sandbox/
git clone https://github.com/swissquote/carnotzet
```

Build the project on the "containers" branch

```bash
cd /work/foundation/pocs/modular-sandbox
hg up containers
mvn clean install
```

Start the carnotzet environment

```bash
cd /work/foundation/pocs/modular-sandox/sq-poc-plugin/sandbox
mvn zet:start
 ```
 
## Troubleshooting

When building your Carnotzet module, you can see the following error :

```
[ERROR] Failed to execute goal com.spotify:docker-maven-plugin:0.4.10:build (build) on project sqi-premium-sandbox: Exception caught: java.util.concurrent.ExecutionException: com.spotify.docker.client.shaded.javax.ws.rs.ProcessingException: org.apache.http.conn.HttpHostConnectException: Connect to 127.0.0.1:2375 [/127.0.0.1] failed: Connection refused -> [Help 1] 
```

This means that the library communicates with the docker daemon in http instead of unix socket. 
To fix this you should check your docker config file: `/etc/systemd/system/docker.service.d/ubuntu.conf` (unbuntu >=15.10) or `/etc/default/docker` (ubuntu <= 15.04). 
And put the correct config like in this setup Docker

TODO :: link to setup


