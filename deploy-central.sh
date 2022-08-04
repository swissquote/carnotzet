#!/usr/bin/env bash
VERSION=1.8.7

# parent pom
mvn gpg:sign-and-deploy-file \
-Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
-DrepositoryId=ossrh \
-DpomFile=pom.xml \
-Dfile=pom.xml

# core
mvn gpg:sign-and-deploy-file \
-Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
-DrepositoryId=ossrh \
-DpomFile=core/pom.xml \
-Dfile=core/target/carnotzet-core-$VERSION.jar

mvn gpg:sign-and-deploy-file \
-Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
-DrepositoryId=ossrh \
-DpomFile=core/pom.xml \
-Dfile=core/target/carnotzet-core-$VERSION-javadoc.jar \
-Dclassifier=javadoc

mvn gpg:sign-and-deploy-file \
-Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
-DrepositoryId=ossrh \
-DpomFile=core/pom.xml \
-Dfile=core/target/carnotzet-core-$VERSION-sources.jar \
-Dclassifier=sources

# file-merger-json
mvn gpg:sign-and-deploy-file \
-Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
-DrepositoryId=ossrh \
-DpomFile=file-merger-json/pom.xml \
-Dfile=file-merger-json/target/carnotzet-file-merger-json-$VERSION.jar

mvn gpg:sign-and-deploy-file \
-Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
-DrepositoryId=ossrh \
-DpomFile=file-merger-json/pom.xml \
-Dfile=file-merger-json/target/carnotzet-file-merger-json-$VERSION-javadoc.jar \
-Dclassifier=javadoc

mvn gpg:sign-and-deploy-file \
-Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
-DrepositoryId=ossrh \
-DpomFile=file-merger-json/pom.xml \
-Dfile=file-merger-json/target/carnotzet-file-merger-json-$VERSION-sources.jar \
-Dclassifier=sources

# docker-compose
mvn gpg:sign-and-deploy-file \
-Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
-DrepositoryId=ossrh \
-DpomFile=docker-compose/pom.xml \
-Dfile=docker-compose/target/carnotzet-orchestrator-docker-compose-$VERSION.jar

mvn gpg:sign-and-deploy-file \
-Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
-DrepositoryId=ossrh \
-DpomFile=docker-compose/pom.xml \
-Dfile=docker-compose/target/carnotzet-orchestrator-docker-compose-$VERSION-javadoc.jar \
-Dclassifier=javadoc

mvn gpg:sign-and-deploy-file \
-Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
-DrepositoryId=ossrh \
-DpomFile=docker-compose/pom.xml \
-Dfile=docker-compose/target/carnotzet-orchestrator-docker-compose-$VERSION-sources.jar \
-Dclassifier=sources

# maven plugin
mvn gpg:sign-and-deploy-file \
-Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
-DrepositoryId=ossrh \
-DpomFile=maven-plugin/pom.xml \
-Dfile=maven-plugin/target/zet-maven-plugin-$VERSION.jar

mvn gpg:sign-and-deploy-file \
-Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
-DrepositoryId=ossrh \
-DpomFile=maven-plugin/pom.xml \
-Dfile=maven-plugin/target/zet-maven-plugin-$VERSION-javadoc.jar \
-Dclassifier=javadoc

mvn gpg:sign-and-deploy-file \
-Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
-DrepositoryId=ossrh \
-DpomFile=maven-plugin/pom.xml \
-Dfile=maven-plugin/target/zet-maven-plugin-$VERSION-sources.jar \
-Dclassifier=sources
