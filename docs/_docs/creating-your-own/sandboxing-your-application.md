---
title: "Sandboxing your application"
permalink: /creating-your-own/sandboxing-your-application
---
To sandbox an existing application.

1. Create sandbox folder in your application with the following structure:

TODO :: check right structure for open source … 

2. Configure the pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
   <parent>
      <groupId>PROJECT_GROUP_ID</groupId>
      <artifactId>PROJECT_ARTIFACT_ID</artifactId>
      <version>1.0.0-SNAPSHOT</version>
   </parent>
   <modelVersion>4.0.0</modelVersion>
   <artifactId>PROJECT_NAME-sandbox</artifactId>
 
   <dependencies>
      …
   </dependencies>
 
   <properties>
      <sq.artifact.type>internal</sq.artifact.type>
      <sandbox.version>1.0.1</sandbox.version>
      <docker.registry>docker.bank.swissquote.ch</docker.registry>
   </properties>
 
   <build>
      <plugins>
          <plugin>
              <groupId>com.swissquote</groupId>
              <artifactId>sq-modular-sandbox-maven-plugin</artifactId>
              <version>${sandbox.version}</version>
              <configuration>
                  <localDeploymentOptions>
                      <enable>true</enable>
                  </localDeploymentOptions>
              </configuration>
         </plugin>
         <!-- copy artifacts for spotify -->
         <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-dependency-plugin</artifactId>
            <version>2.10</version>
            <executions>
               <execution>
                  <id>copy</id>
                  <phase>package</phase>
                  <goals>
                     <goal>copy</goal>
                  </goals>
                  <configuration>
                     <outputDirectory>${project.build.directory}/docker</outputDirectory>
                     <overWriteReleases>false</overWriteReleases>
                     <overWriteSnapshots>true</overWriteSnapshots>
                     <artifactItems>
                        <!-- service war -->
                        <artifactItem>
                           <groupId>${project.groupId}</groupId>
                           <artifactId>PROJECT_ARTIFACT_ID</artifactId>
                           <version>${project.version}</version>
                           <type>war</type>
                           <destFileName>PROJECT_NAME.war</destFileName>
                        </artifactItem>
                     </artifactItems>
                  </configuration>
               </execution>
            </executions>
         </plugin>
 
         <plugin>
            <groupId>com.spotify</groupId>
            <artifactId>docker-maven-plugin</artifactId>
            <configuration>
               <imageName>${docker.registry}/PROJECT_NAME:${project.version}</imageName>
               <dockerDirectory>${project.basedir}/src/main/resources/docker</dockerDirectory>
               <resources></resources>
            </configuration>
 
            <executions>
               <execution>
                  <id>build</id>
                  <phase>install</phase>
                  <goals>
                     <goal>build</goal>
                  </goals>
               </execution>
               <execution>
                  <id>push-image</id>
                  <phase>deploy</phase>
                  <goals>
                     <goal>push</goal>
                  </goals>
               </execution>
            </executions>
         </plugin>
      </plugins>
   </build>
</project>
```

3. In Dockerfile describe how your application is supposed to start (using [this reference](https://docs.docker.com/engine/reference/builder/))
   For example:
   ```
   FROM docker.bank.swissquote.ch/sq-java8-tomcat8
   ADD PROJECT_ARTIFACT_ID.war /softwares/tomcat/webapps/PROJECT_ARTIFACT_ID.war
   ```

4. Add environment variables with [this documentation](/creating-your-own/configuration-using-environment-variables).
5. After configuration run terminal from `sandbox` folder:
   ```bash
   mvn clean install
   ```
   
   it will generate docker-compose.yml file for docker-compose to link dependent containers if thay exist;
  
   ```bash
   mvn zet:start
   ```
   
   it will up the container.
   

For configuration dependent applications look [here](/creating-your-own/configuration-using-config-files).
