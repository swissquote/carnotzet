---
title: "Extending carnotzet features"
url: /extending-behavior/extension-points
---

{% include toc %} 

## Static extension

This extension points allows you to modify the applications in the environment, you can for example:
- Add or remove applications to the environment
- Add volume mounts to containers in the environment
- Override the entrypoint/cmd of the container, for example to wrap the startup script of containers with custom code
- etc...

Have a look at examples in the /examples/hello-extension folder to see how it is done.

## Runtime extension

Sometimes, extending the definition of the environment is not enough and you need to use runtime information in your
extensions (for example the instance id or the Ip addresses of running containers).

To achieve this, there is another extension point for ContainerRuntimes which allows you to extend behavior around the
lifecycle of containers and use the information only available at runtime.

Have a look at examples in the /examples/hello-runtime-extension folder to see how it is done.


## Enabling and configuring extensions

Extensions can be enabled by adding them to the classpath or manually registering them in the CarnotzetConfig.
When using the plugin, they can also be configured individually using the pom.xml. Have a look at the /exmaples/redis 
carnotzet to see how.
