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

The following items must be completed, in the following order:

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

# Settings

The procedures in this document will use the folowing settings.  You will need to change some of these settings to match your test environment.

## MongoDB

- Default Password: `password`
- Port: `27017`

## Tomcat instances

- Access Manager: port: `18080`
- Applications (Content Server and Resoure Server): port: `38080`

## Access Manager

- Admin User: user id: `amadmin`, password: `password`
- Resource Server (RS): client id: `UMA-Resource-Server`, client secret: `password`
- Requesting Party (RqP): client id: `UMA-Rqp-Client`, client secret: `password`
- Resource Owner (RO): user id: `dcrane`, password: `password`
- Requesting Party (RqP): user id: `bjensen`, password: `password`

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

The ForgeRock Access Manager (6.0.x, 6.5.x) needs to be configured to support UMA 2.0 Authorization Server (AS) functionality. The ForgeRock Access Manager Policy APIs and OAuth 2.0 functionality will also configured. See the Access Manager 6.5 [User Managed Access (UMA) 2.0 Guide](https://backstage.forgerock.com/docs/am/6.5/uma-guide/) for installation details.

These procedures will create and configure:
- **OAuth2 Provider** 
- **UMA Provider**
- **OAuth 2.0 Client Agent**, application used by the Requesting Party (RqP)
- **OAuth 2.0 Resource Server (RS)**
- **Resource Owner (RS)**, the user, `dcrane`, that owns the resources
- **Requesting Party (RqP)**, the user, `bjensen`, that requests and gets access to the resources

See the Access Manager 6.5 [UMA Setup Procedures](https://backstage.forgerock.com/docs/am/6.5/uma-guide/#uma-set-up-procedures) documentation for details

Log into the Access Manager admin console as ``amadmin``

## Create Services

This procedure will create two **Services**:
- OAuth2 Provider
- UMA Provider

**NOTICE:** *If you are using an existing Access Manager installation and these Providers exist, they will be replaced.*

1. From **Top Menu Bar**, select `Realms` > `Top Level Realm` 
1. From panel, select `Configure OAuth Provider` 
1. From panel, select `Configure User Managed Access` 
1. Check (enable) `Issue Refresh Tokens` 
1. Check (enable) `ISsue Refresh Tokens on Refreshing Access Tokens` 
1. Click `Create` 
1. From the dialog window, click `OK`

## Create *UMA Client / Requesting Party (RqP)* Agent

This procedure creates / configures an OAuth 2.0 client for the Requesting Party (RqP) application which will access resources.

1. From **Top Menu Bar**, select `Realms` > `Top Level Realm` 
1. From the Left Menu, select **Applications** > **OAuth 2.0** 
1. Select **Clients** Tab 
1. Click **+ Add Client** 
1. Set **Client ID** to `UMA-RqP-Client` 
1. Set **Client Secret** `password` 
1. Set **Scopes** `read` and `openid` *(press Enter after each item)*
1. Click **Create** 
1. Select **Advanced** Tab 
1. Set **Display Name** to `UMA RqP` 
1. Set **Display Description** to `User Managed Access (UMA) Requesting Party Client` 
1. Set **Grant Type** to include: `Authorization Code` and `UMA` *(press Enter after each item)*
1. Click **Save Changes**

## Create *UMA Resource Server (RS)* Agent

This procedure creates / configures an OAuth 2.0 client for the Resource Server (RS) application.

1. From **Top Menu Bar**, select `Realms` > `Top Level Realm` 
1. From the Left Menu, select **Applications**, Select **OAuth 2.0** 
1. Select **Clients** Tab 
1. Click **+ Add Client** 
1. Set **Client ID** to `UMA-Resource-Server` 
1. Set **Client Secret** `password` 
1. Set **Scopes** `uma_protection` *(press Enter after each item)*
1. Click **Create** 
1. Select **Core** Tab 
1. Set **Refresh Token Lifetime (seconds)** to `-1` 
1. Click **Save Changes** 
1. Select **Advanced** Tab 
1. Set **Display Name** to `UMA RS` 
1. Set **Display Description** to `User Managed Access (UMA) Resource Server` 
1. Set **Grant Type** to include: `Authorization Code` and `Refresh Token` 
1. Click **Save Changes**

## Create *Resource Owner (RO)* User

1. From **Top Menu Bar**, select `Realms` > `Top Level Realm` 
1. From the Left Menu, select **Identities**
1. Select **Identities** Tab
1. Click **+ Add Identity**
1. Set **User ID** to `dcrane`
1. Set **Password** to `password`
1. Click **Create**
1. Set **First Name** to `Danny`
1. Set **Last Name** to `Crane`
1. Set **Full Name** to `Danny Crane - Resource Owner`
1. Set **User Status** to `Active`
1. Click **Save Changes**

## Create *Requesting Party (RqP)* User

1. From **Top Menu Bar**, select `Realms` > `Top Level Realm` 
1. From the Left Menu, select **Identities**
1. Select **Identities** Tab
1. Click **+ Add Identity**
1. Set **User ID** to `bjensen`
1. Set **Password** to `password`
1. Click **Create**
1. Set **First Name** to `Barb`
1. Set **Last Name** to `Jensen`
1. Set **Full Name** to `Barb Jensen - Requesting Party`
1. Set **User Status** to `Active`
1. Click **Save Changes**

# Install Resource Server

This example deploys the `resource-server.war` file to an Apache Tomcat 8.x environment.

## Deploy war file

Copy the `resource-server.war` file to the `webapps` folder in the Tomcat server installation.  The running Tomcat server will automatically unpack the war file.

```
cp ./target/resource-server.war TOMCAT_INSTALLATION/webapps
```

## Configure

The deployed application needs to be configured.  Edit the `resource-server.json` file and change /check the values.

```
cd TOMCAT_INSTALLATION/webapps/resource-server/WEB-INF/config
vi resource-server.json
```
Edti the following sections of the JSON file:

### Resource Server (RS) Connection: `rs.connet`

```
      "connect": {
         "protocol": "_PROTOCOL_",
         "host": "_HOSTNAME_",
         "port": "_APP_PORT_",
         "deploy": "resource-server",
         "endpoint": "rest"
      },
```
- Set **protocol**: `http` or `https`
- Set **host**: Fully Qualified Domain Name (FQDN) of installation
- Set **port**: Port for Tomcat instance that has the Resource Server installed: `38080`

### Resource Server (RS): No SQL Database (MongoDB): `rs.nosql`

```
      "nosql": {
         "comment": "No SQL Database (MongoDB)",
         "host": "_HOSTNAME_",
         "port": "27017",
         "authen": {
            "database": "resource-server",
            "user": "resourceadmin",
            "password": "_PASSWORD_"
         },
         ...
      },
```

- Set **host**: Fully Qualified Domain Name (FQDN) of installation
- Set **password**: Password for the MongoDB resource-server database: `password`

### Resource Server (RS): OAuth 2.0 Client: `rs.nosql`

```
      "oauth2": {
         "scopes": "uma_protection",
         "client": {
            "id": "UMA-Resource-Server",
            "secret": "_PASSWORD_",
            "redirect": "_PROTOCOL_://_HOSTNAME_:_APP_PORT_/resource-server/callbacks/oauth2.html"
         }
      },
```

- Set **secret**: Password for the OAuth 2.0 Client: `password`
- Set **redirect PROTOCOL**: `http` or `https`
- Set **redirect HOSTNAME**: Fully Qualified Domain Name (FQDN) of installation
- Set **redirect APP_PORT**: Port for Tomcat instance that has the Resource Server installed: `38080`

### Authorization Server (AS) Connection: `as.connect`

```
      "connect": {
         "protocol": "_PROTOCOL_",
         "host": "_HOSTNAME_",
         "port": "_AM_PORT_",
         "path": "openam"
      },
```

- Set **protocol**: `http` or `https`
- Set **host**: Fully Qualified Domain Name (FQDN) of installation
- Set **port**: Port for Tomcat instance that has the Access Manager installed: `18080`

### Authorization Server (AS) admin credentials: `as.admin`

```
      "admin": {
         "user": "amadmin",
         "password": "_PASSWORD_"
      },
```

- Set **password**: Password for the Access Manager adminstrator account: `password`

### Content Server (CS) Connection: `cs.connect`

```
      "connect": {
         "protocol": "_PROTOCOL_",
         "host": "_HOSTNAME_",
         "port": "_APP_PORT_",
         "path": "content-server/rest/content-server/content"
      }
```

- Set **protocol**: `http` or `https`
- Set **host**: Fully Qualified Domain Name (FQDN) of installation
- Set **port**: Port for Tomcat instance that has the Content Server installed: `38080`

Restart the Tomcat server running the **Resource Server** (`resource-server`)

