---
title: "Generate Carnotzet module from SAP Archetype"
permalink: /creating-your-own/generate-carnotzet-module-from-sap-archetype
---

Clone the SAP archetype and build it:
   
```bash
git clone https://github.com/swissquote/carnotzet
cd carnotzet/
mvn clean install
```

Change directory to your project folder and run generator;
    
```bash
cd ~/PROJECT_FOLDER
mvn archetype:generate
```

Next follow the instructions.

This will create a Carnotzet module in your application.

