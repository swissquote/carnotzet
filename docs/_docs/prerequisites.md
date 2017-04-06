---
title: "Prerequisites"
permalink: /prerequisites
---

1. Make sure you are using java >= 8 and maven  >= 3.3.X
1. Make sure you have docker installed : [https://docs.docker.com/engine/installation/](https://docs.docker.com/engine/installation/).
1. Make sure you have docker-compose installed : [https://docs.docker.com/compose/install/](https://docs.docker.com/compose/install/)
1. Make sure you have DNSDock installed : [](Docker - DNS Dock)
1. Make sure your maven settings allow usage of docker : [](Docker - Maven plugin)
1. Make sure all private internet addresses as well as the .docker domain are not proxied by your cntlm config (/etc/cntlm.conf), something like this :

1. Make sure your CNTLM proxy is listening on the docker0 interface :

   ```bash
   export DOCKER_INTERFACE_ADDRESS=$(ip -f inet -o addr show docker0|cut -d ' ' -f 7 | cut -d '/' -f 1)
   if
     ! grep $DOCKER_INTERFACE_ADDRESS /etc/cntlm.conf;
   then
     echo "Listen $DOCKER_INTERFACE_ADDRESS:3129" >> /etc/cntlm.conf && service cntlm restart;
   fi
   ```

1. Ensure that you have `com.swissquote` as a `pluginGroup` in your maven `~/.m2/settings.xml`

   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <settings xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
           http://maven.apache.org/xsd/settings-1.0.0.xsd">
   …
   <pluginGroups>
       …
       <pluginGroup>com.swissquote</pluginGroup>
       …
   </pluginGroups>
   …
   </settings>
   ```
