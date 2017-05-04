---
title: "Dependencies"
permalink: /creating-your-own/dependencies
---
{% include toc %}

## Environment composition

Adding a maven dependency from a carnotzet module to another imports the whole environment. 
You can compose your environment by adding multiple dependencies and you can use maven's dependency management 
features to choose versions.

For example, let's imagine we have an application named "app1" which depends on redis and postgres. 
The dependencies of the maven module would look like:
```xml
<dependencies>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>redis-carnotzet</artifactId>
        <version>3</version>
    </dependency>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>postgres-carnotzet</artifactId>
        <version>9.4</version>
    </dependency>
</dependencies>
```

The environment will contain app1, redis and postgres:

```
         +------+
         | app1 |
         +-+--+-+
           |  |
           |  |
           |  |
+-------+  |  |    +----------+
| redis +--+  +----| postgres |
+-------+          +----------+
```

Now if another team wants to import "app1" into the environment of "app2" (let's say it provides them a REST api, and we want 
to avoid integration surprises so they don't want to mock it). This other team can depend on the app1 module and redis
 and postgres will transitively become part of their dev/test environment.
 
```xml
<dependencies>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>app1-carnotzet</artifactId>
        <version>1</version>
    </dependency>
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>mysql-carnotzet</artifactId>
        <version>5</version>
    </dependency>
</dependencies>
```

The final environment for app2 will contain mysql, postgres, redis, app1 and app2 : 

```
+------+
| app2 +------------+
+-+----+            |
  |                 |
  |                 |
+-+----+          +---+---+
| app1 +----+     | mysql |
+--+---+    |     +-------+
   |        |
   |        |
+--+----+   |    +----------+
| redis |   +----+ postgres |
+-------+        +----------+
```