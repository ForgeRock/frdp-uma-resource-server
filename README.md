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

# Build

## Prerequisite:

The following items must be completed, in order:

1. [frdp-framework](https://github.com/ForgeRock/frdp-framework) ... clone / download then install using *Maven* (`mvn`)
1. [frdp-dao-mongo](https://github.com/ForgeRock/frdp-dao-mongo) ... clone / download then install using *Maven* (`mvn`)
1. [frdp-content-server](https://github.com/ForgeRock/frdp-content-server) ... clone / download then install using *Maven* (`mvn`)
1. [frdp-dao-rest](https://github.com/ForgeRock/frdp-dao-rest) ... clone / download then install using *Maven* (`mvn`)

## Clean, Compile, Install:

Run *Maven* (`mvn`) processes to clean, compile and install the package:

```
mvn clean
mvn compile 
mvn package
```

The *package* process creates a deployable war file, in the current directory: `./target/resource-server.war`: 

```
ls -la ./target
total 24104
drwxr-xr-x   6 scott.fehrman  staff       192 Jan  6 20:15 .
drwxr-xr-x  14 scott.fehrman  staff       448 Jan  6 20:15 ..
drwxr-xr-x   3 scott.fehrman  staff        96 Jan  6 20:15 classes
drwxr-xr-x   3 scott.fehrman  staff        96 Jan  6 20:15 maven-archiver
drwxr-xr-x   5 scott.fehrman  staff       160 Jan  6 20:15 resource-server
-rw-r--r--   1 scott.fehrman  staff  12340457 Jan  6 20:15 resource-server.war
```

# Configure MongoDB

The MongoDB object database needs to be configured for the **resources** and **credentials** collections in the **resource-server** database.

1. Access MongoDB system \
`ssh root@hostname`
1. Connect as "root" user to create database and collection \
`mongo --username "root" --password "<ROOT_PASSWORD>" --authenticationDatabase "admin" admin`
1. Specify the database name \
`> use resource-server;`
1. Drop existing database \
`> db.dropDatabase();`
1. Drop existing admin user \
`> db.dropUser("resourceadmin");`
1. Create admin user \
`> db.createUser({user:"resourceadmin",pwd:"password",roles:["readWrite","dbAdmin"]});`
1. Create *credentials* collection \
`> db.createCollection("credentials");`
1. Create *resources* collection \
`> db.createCollection("resources");`
1. Logout as the "root" user \
`> quit();`
1. Connect as the "resourceadmin" user \
`mongo --username "resourceadmin" --password "password" --authenticationDatabase "resource-server" resource-server`
1. Create index in the *credentials* collection \
`> db.credentials.createIndex({"uid":1});` \
`> db.credentials.createIndex({"data.owner":1}, {unique: true});`
1. Insert sample record into the *credentials* collection \
`> db.credentials.insert({"comment": "This is a test document"});`
1. Display the sample record \
`> db.credentials.find();` \
`> db.credentials.find().pretty();`
1. Create index in the *resources* collection \
`> db.resources.createIndex({"uid":1});` \
`> db.resources.createIndex({"data.owner":1});` \
`> db.resources.createIndex({"data.register":1});`
1. Insert sample record into the *resources* collection \
`> db.resources.insert({"comment": "This is a test document"});`
1. Display the sample record \
`> db.resources.find();` \
`> db.resources.find().pretty();`
1. Logout \
`> quit();`

# Configure Access Manager

The ForgeRock Access Manager needs to be configured to support UMA 2.0 (and OAuth 2.0)

