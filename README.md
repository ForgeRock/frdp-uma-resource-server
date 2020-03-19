# frdp-uma-resource-server

ForgeRock Demonstration Platform : **UMA Resource Server** : A deployable web service that provides REST / JSON operations for the [User Managed Access (UMA) 2.0](https://kantarainitiative.org/confluence/display/uma/Home)  Resource Server (RS) functionality.  This service is implemented using the Java JAX-RS/Jersey REST API and MongoDB for document persistance. This service also leverages the [ForgeRock Access Manager](https://www.forgerock.com/platform/access-management) for the UMA 2.0 Authorization Server (AS) functionality.

![overview image](images/overview.png)

`git clone https://github.com/ForgeRock/frdp-uma-resource-server.git`

# Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

# License

[MIT](/LICENSE)

# Reference 

- Specification: [User-Managed Access (UMA) 2.0 Grant for OAuth 2.0 Authorization](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-grant-2.0.html)
- Specification: [Federated Authorization for User-Managed Access (UMA) 2.0](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-federated-authz-2.0.html)
- Documentation: [ForgeRock Access Manager, User Managed Access (UMA) 2.0 Guide](https://backstage.forgerock.com/docs/am/6.5/uma-guide/)

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

The procedures in this document will use the following settings.  You will need to change some of these settings to match your test environment.

| Technology   | Category | Name      | Value      |
| ------------ | -------- | --------- | ---------- |
| **MongoDB**   |
|              | Default 
|              |          | Password:  | `password` |
|              |          | Port:      | `27017`    |
| **Tomcat** |
|              | Access Manager |
|              |               | HTTP Port:  | `18080` |
|              |               | HTTPS Port: | `18443`
|              | Applications  |
|              |               | HTTP Port: | `38080` |
|              |               | HTTPS Port: | `38443` |
| **Access Manager** |
|                | Admin User 
|                |            | User Id: | `amadmin` |
|                |            | Password: | `password` |
|                | OAuth Client: Resource Server |
|                |                 | Client Id: | `UMA-Resource-Server` |
|                |                 | Client Secret: | `password` |
|                | OAuth Client: Requesting Party |
|                |                 | Client Id: | `UMA-RqP-Client` |
|                |                 | Client Secret: | `password` |
|                | User: Resource Owner |
|                |                 | User Id: | `dcrane` |
|                |                 | Password: | `password` |
|                | User: Requesting Party |
|                |                 | User Id: | `bjensen` |
|                |                 | Password: | `password` |

# Configure MongoDB

The MongoDB object database needs to be configured for the **resources** and **credentials** collections in the **resource-server** database.

1. Access MongoDB system \
\
`ssh root@hostname`
1. Connect as the "root" MongoDB user to create the database and collections \
\
`mongo --username "root" --password "<ROOT_PASSWORD>" --authenticationDatabase "admin" admin`
1. We need to do some database initialization ... 
Specify the database name: `resource-server`.
Drop database if it already exists. 
Create an admin user, remove first, for the database: `resourceadmin`. 
Create two collections: `credentials` and `resources`. Quit MongoDB. \
\
`use resource-server;` \
`db.dropDatabase();` \
`db.dropUser("resourceadmin");` \
`db.createUser({user:"resourceadmin",pwd:"password",roles:["readWrite","dbAdmin"]});` \
`db.createCollection("credentials");` \
`db.createCollection("resources");` \
`quit();`

1. Connect as the "resourceadmin" user for the `resource-server` database.\
\
`mongo --username "resourceadmin" --password "password" --authenticationDatabase "resource-server" resource-server`
1. Create indexes for both the `resources` and `credentials` collections. 
Insert test documents into both collections. 
Read the documents from both collections. Quit MongoDB. \
\
`db.resources.createIndex({"uid":1});` \
`db.resources.createIndex({"data.owner":1});` \
`db.resources.createIndex({"data.register":1});` \
`db.credentials.createIndex({"uid":1});` \
`db.credentials.createIndex({"data.owner":1}, {unique: true});` \
`db.resources.insert({"comment": "This is a test document"});` \
`db.credentials.insert({"comment": "This is a test document"});` \
`db.resources.find();` \
`db.resources.find().pretty();` \
`db.credentials.find();` \
`db.credentials.find().pretty();` \
`quit();`

# Configure Access Manager

The ForgeRock Access Manager (6.0.x, 6.5.x) needs to be configured to support the UMA 2.0 Authorization Server (AS) functionality. The ForgeRock Access Manager Policy APIs and OAuth 2.0 functionality will also configured. See the Access Manager 6.5 [User Managed Access (UMA) 2.0 Guide](https://backstage.forgerock.com/docs/am/6.5/uma-guide/) for installation details.

These procedures will create and configure:
- **OAuth2 Provider** 
- **UMA Provider**
- **OAuth 2.0 Client Agent**, application used by the Requesting Party (RqP)
- **OAuth 2.0 Resource Server (RS)**
- **Resource Owner (RO)**, the user, `dcrane`, that owns the resources
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

## Create OAuth 2.0 *UMA Requesting Party (RqP)* Client

This procedure creates an OAuth 2.0 client for the Requesting Party (RqP) application which will access resources.

1. From **Top Menu Bar**, select `Realms` > `Top Level Realm` 
1. From the Left Menu, select **Applications** > **OAuth 2.0** 
1. Select **Clients** Tab 
1. Click **+ Add Client** 
1. Set **Client ID** to `UMA-RqP-Client` 
1. Set **Client Secret** `password` 
1. Set **Redirection URIs** `_PROTOCOL_://_HOSTNAME_:_PORT_/resource-server`
1. Set **Scopes** `read` and `openid` *(press Enter after each item)*
1. Click **Create** 
1. Select **Advanced** Tab 
1. Set **Display Name** to `UMA RqP` 
1. Set **Display Description** to `User Managed Access (UMA) Requesting Party Client` 
1. Set **Grant Type** to include: `Authorization Code` and `UMA` *(press Enter after each item)*
1. Click **Save Changes**

## Create OAuth 2.0 *UMA Resource Server (RS)* Client

This procedure creates an OAuth 2.0 client for the Resource Server (RS) application.

1. From **Top Menu Bar**, select `Realms` > `Top Level Realm` 
1. From the Left Menu, select **Applications**, Select **OAuth 2.0** 
1. Select **Clients** Tab 
1. Click **+ Add Client** 
1. Set **Client ID** to `UMA-Resource-Server` 
1. Set **Client Secret** `password`
1. Set **Redirection URIs** `_PROTOCOL_://_HOSTNAME_:_PORT_/resource-server`
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

```bash
cp ./target/resource-server.war TOMCAT_INSTALLATION/webapps
```

## Configure `resource-server.json`

The deployed Resource Server application needs to be configured.  Edit the `resource-server.json` file and change / check the values.

```bash
cd TOMCAT_INSTALLATION/webapps/resource-server/WEB-INF/config
vi resource-server.json
```
Edit the following sections of the JSON file:

### Resource Server (RS): No SQL Database (MongoDB): 
JSON Object ... `rs.nosql`:

```json
{
   "nosql": {
      "comment": "No SQL Database (MongoDB)",
      "host": "uma.example.com",
      "port": "27017",
      "authen": {
         "database": "resource-server",
         "user": "resourceadmin",
         "password": "password"
      }
   }
}
```

- Set `host`: Fully Qualified Domain Name (FQDN) of installation
- Set `port`: Port for MongoDB service, default is *27017*
- Set `password`: Password for MongoDB resource-server, *resourceadmin* user

### Resource Server (RS): OAuth 2.0 Client: 
JSON Object ... `rs.oauth2.client`

```json
{
   "client": {
      "id": "UMA-Resource-Server",
      "secret": "password",
      "redirect": "https://uma.example.com:443/resource-server"
   }
}
```

- Set **secret**: Password for the OAuth 2.0 Client: `password`
- Set **redirect**: This value **MUST** match the redirect when configuring the OAuth 2.0 client

#### NOTICE:  The `client` attributes MUST match the values used when the Access Manager OAuth 2.0 Client `UMA-Resource-Server` configuration:

- `id`
- `secret`
- `redirect`

### Authorization Server (AS) Connection: 
JSON Object ... `as.connect`

```json
{
   "connect": {
      "protocol": "https",
      "host": "uma.example.com",
      "port": "443",
      "path": "openam"
   }
}
```

- Set **protocol**: `http` or `https`
- Set **host**: Fully Qualified Domain Name (FQDN) of installation
- Set **port**: Port that has the Access Manager installed: `443`

### Authorization Server (AS) admin credentials: 
JSON Object ... `as.admin`

```json
{
   "admin": {
      "user": "amadmin",
      "password": "password"
   }
}
```

- Set **password**: Password for the Access Manager administrator account: `password`

## Configure `content-services.json`

The deployed Resource Server application needs to be configured.  The Resource Server uses a separate file for the configuration of *content services*.  These instruction will cover the configuration of the `default` Content Service.  Edit the `content-services.json` file and change / check the values.

There's an *array* of `services` we will change the service that has an `id` value of *default*

JSON Object ... `operations.create`

```json
{
   "create": {
      "comment": "Use 'uri' attribute and input 'data' to CREATE external content",
      "action": "post",
      "uri": "https://uma.example.com:443/content-server/rest/content-server/content"
   }
}
```

- Set **uri**: This is the full FQDN path for where content is created, via HTTP POST

### Note: 
- See the [Configurable Content Service project](https://github.com/ForgeRock/frdp-uma-resource-server/wiki/Project:-Configurable-Content-Service) for details on configuring the **Content Service**

## Restart the Tomcat server running the **Resource Server** (`resource-server`)

# Testing

The following Use Cases will test the UMA 2.0 capabilities and "value add" features of the Resource Server:

| Actor            | Use Case       | Category | Description |
| ---------------- | -------------- | -------- | ----------- |
| Resource Owner   | 
|                  | Resources | UMA 2.0 | Create, Read, Update, Delete resources (meta, content, registration, policies).
|                  | People | Value Add | Who has access to my resources.
|                  | Requests | Value Add | Accessing a resource, without permission, will generate a access request.
| Requesting Party | 
|                  | Resources | UMA 2.0 | Accessing shared resources
|                  | Shared With Me | Value Add | What resource do I currently have access to
|                  | Discovery | Value Add | What resources are discoverable
|                  | Revoke Access | Value Add | RqP initiated access revocation

Use the provided Postman [collections](/testing) to test the Use Cases:

- [UMA Resource Owner](/testing/RO/README.md)
- [UMA Requesting Party](/testing/RqP/README.md)

