# frdp-uma-resource-server

ForgeRock Demonstration Platform : **UMA Resource Server** : A deployable web service that provides REST / JSON operations for the [User Managed Access (UMA) 2.0](https://kantarainitiative.org/confluence/display/uma/Home)  Resource Server (RS) functionality.  This service is implemented using the JAX-RS/Jersey REST API and MongoDB for document persistance. This service also leverages the [ForgeRock Access Manager](https://www.forgerock.com/platform/access-management) for the UMA 2.0 Authorization Server (AS) functionality.

`git clone https://github.com/ForgeRock/frdp-uma-resource-server.git`

# Requirements

The following items must be installed:

1. [Apache Maven](https://maven.apache.org/) *(tested with 3.5.x, 3.6.x)*
1. [Java Development Kit 8](https://openjdk.java.net/)
1. [MongoDB](https://www.mongodb.com) *(tested with 3.2)*
1. [Apache Tomcat](https://tomcat.apache.org/index.html) *(tested with Tomcat 8.5.x)*
1. [ForgeRock Access Manager](https://www.forgerock.com/platform/access-management) *(tested with 6.0, 6.5)*

