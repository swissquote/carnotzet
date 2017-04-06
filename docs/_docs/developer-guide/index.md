---
title: "Developer Guide"
permalink: /developer-guide/
---

## FAQ

__I run mvn clean install and this error "Cannot initiate the connection to archive.ubuntu.com:80" is triggered, what should I do?__

If you happen to be in gland the proxy is giving some hard time, try the profile named "gland", run : mvn clean install -P gland
