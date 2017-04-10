---
title: "Useful commands"
permalink: /user-guide/useful-commands
---

{% include toc %}

## Check memory consumed per docker images
```bash
docker stats $(docker ps --format={{ "{{.Names" }}}})
```

 
## Delete all images

```bash

#! /bin/bash
# delete all images except sq-base-image and dnsdock
 
CMD_PS="docker ps | awk '{if(NR>1)print}' | grep -v dnsdock | awk '{print \$1}'";
CMD_CONTAINERS="docker ps -a | awk '{if(NR>1)print}' | grep -v dnsdock | awk '{print \$1}'";
CMD_IMAGES="docker images | awk '{if(NR>1)print}' | grep -v sq-base-image | grep -v dnsdock | awk '{print $3}'";
 
echo "Stopping containers";
LIST_RUNNING=$(eval $CMD_PS);
if [ ${#LIST_RUNNING} -ne 0 ] ;
then docker kill $LIST_RUNNING
fi
 
echo "Removing containers";
LIST_CONTAINERS=$(eval $CMD_CONTAINERS);
if [ ${#LIST_CONTAINERS} -ne 0 ] ;
then docker rm $LIST_CONTAINERS
fi
 
echo "Removing images";
LIST_IMAGES=$(eval $CMD_IMAGES);
if [ ${#LIST_IMAGES} -ne 0 ] ;
then docker rmi $LIST_IMAGES
fi
```
 
## Debugging a running container
```
~/bin$ docker ps -a | grep sqc-game-soa
4409be7688af        docker.bank.swissquote.ch/sqc-game-soa:1.0.0-SNAPSHOT                          "/wait-for-it.sh --ti"   About a minute ago   Up About a minute   80/tcp, 4343/tcp, 8000/tcp, 9000/tcp   itests-sqc-web-game-security-filter_sqc-game-soa
~/bin$ docker exec -it 1835b3e3527a /bin/bash
```
 
## Debugging a stopped container
```
~/bin$ docker images | grep sq-java8-tomcat8
docker.bank.swissquote.ch/sq-java8-tomcat8         v7                  e4b257bbc7c5        3 months ago        190.8 MB
 
~/bin$ docker run --rm -it e4b257bbc7c5 /bin/bash
```
