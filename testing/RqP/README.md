# UMA Requesting Party

This document covers how to test **UMA Requesting Party (RqP)** operations using the **Resource Server (RS)** 

# Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

# Reference 

- Specification: [User-Managed Access (UMA) 2.0 Grant for OAuth 2.0 Authorization](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-grant-2.0.html)
- Specification: [Federated Authorization for User-Managed Access (UMA) 2.0](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-federated-authz-2.0.html)
- Documentation: [ForgeRock Access Manager, User Managed Access (UMA) 2.0 Guide](https://backstage.forgerock.com/docs/am/6.5/uma-guide/)

# Setup

This test uses the [Postman](https://www.getpostman.com/downloads/) utility to execute REST/JSON commands with the **Resource Server (RS)** and the **Authorization Server (AS)**.

Set Preferences:

- General:\
Disable SSL certificate verification

Load Environment Variables *(may have been loaded from the **RO** test):*
- `../Resource_Server.postman_environment.json`

Load Collections: 
- `UMA_RqP.postman_collection.json`

Set Postman Environment variables for the **Resource Server** ... NOTE: set both the **`INITIAL VALUE`** and **`CURRENT VALUE`** the same.
- `FQDN-openam`:\
Set your **Authorization Server (AS)** deployment, *ForgeRock Access Manager*\
Example: `https://uma.example.com:443/openam`
- `FQDN-rs`:\
Set your **Resource Server (RS)** deployment\
Example: `https://uma.example.com:443/resource-server`

Remove **all** the Cookies

# Overview

The **Requesting Party (RqP)** is a user that wants access to a `resource` that is *managed* by a **Resource Owner (RO)**.

A `resource` can represent anything.  This project uses a *document* concept to simulate a `resource` that is being shared.

The **UMA** protocol defines a specific *flow* for how a `resource` is accessed.  The *flow* contains the following steps:

| Step | Description 
| ---- | ----------- 
| Submit Request | Submit a request which includes the `resource Id` and the `scopes`.  This request *will fail* because a valid **Requesting Party Token (RPT)** is not provided.  A **UMA Permission Ticket** is returned and will be used to create a new **RPT**.
| Get Claim Token | A **UMA Claim Token** (an OAuth 2.0 access_token) needs to obtained from the **Authorization Server (AS)**.  This is done by getting an `authorization_code`, from the SSO token.  The `authorization_code` is then used to get the **UMA Claim Token** 
| Get Requesting Party Token | A **UMA Requesting Party Token (RPT)** is obtained from the the **Authorization Server (AS)** using the **UMA Permission Ticket** and the **UMA Claim Token**
| Re-Submit Request | Submit a request which includes the `resource Id`, `scopes` and a valid **Requesting Party (RPT)**.  The **RPT** is verified by the **Resources Server (RS)**, which contacts the **Authorization Server (AS)**.  The **RS** returns a JSON response with data based on the `scopes`.

# Authenticate

The **Requesting Party (RqP)** needs to authenticate to the **Authorization Server (AS)**, *ForgeRock Access Manager*, and obtain a Single-Sign-On (SSO) token: `tokenId`

1. Open the **UMA Requesting Party** Postman Collection

1. Open the **Authenticate** Folder

1. Select the **Login** command \
This is a `POST` method that uses Header variables: `X-OpenAM-Username` and `X-OpenAM-Password` to authenticate the user **(RqP)**

1. Click **Send** \
A JSON payload is returned: \
```{"tokenId":"...","successUrl":"/openam/console","realm":"/"}```\
The `tokenId` attribute is saved as a Postman environment variable: `RqPtokenId`. \
The **Authorization Server (AS)**, *ForgeRock Access Manager*, sets a Cookie: `iPlanetDirectoryPro`

1. Select the **Validate** command \
This is a `POST` method that uses the Cookie `iPlanetDirectoryPro` and validates that the user's session is still valid.

1. Click **Send** \
A valid session will return a JSON payload: \
```{"valid":true,"sessionUid":"...","uid":"bjensen","realm":"/"}```\
The `uid` value **MUST** be `bjensen`, the **Requesting Party (RqP)**

1. DO NOT CONTINUE WITHOUT A VALID SSO SESSION

# Get Resource 

The **Requesting Party (RqP)** uses a **Client Application (CA)**, which is an OAuth 2.0 client, to obtain the `resource`.  The Postman utility is *acting* as the **CA**.

1. Open the **UMA Requesting Party** Postman Collection

1. Open the **Get Resource** Folder

1. Select the **1: Submit Request** command \
This is a `GET` method that uses the Header variable: `x-frdp-ssotoken` set to the `RqPtokenId` value. \
The `resourceId` is in URL path. \
The `scopes` are set as a URL query parameter. \
Example: `https://{{FQDN}}/resource-server/rest/share/resources/{{resourceId}}/?scopes={{scopes}}`

1. Click **Send** \
The `request` will fail, due to a missing (or invalid) **Requesting Party Token (RPT)** \
The `response` will contain a JSON payload.  The **Permission Ticket**, (`ticket` attribute) and the **AS URI** (`as_uri` attribute) will be saved as Postman environment variables.

1. Select the **2: Get Authz Code** command \
This is a `POST` method that uses the Header variable: `iPlanetDirectoryPro` set to the `RqPtokenId` value. \
It calls the OAuth 2.0 `/authorize` endpoint. \
It uses `x-www-form-urlencoded` body. 

1. Click **Send** \
The response is a HTML Form that contains a `hidden` input attribute, which contains the authorization code. \
Example: ```<input type='hidden', name='code', value='...'>``` \
The `code` value is saved as a Postman Environment Variable: `authzCode` \
The `basicAuth` variable is create by encoding the OAuth `clientId` and `clientSecret`.  This is used in the next step for Basic Authentication

1. Select the **3: Get Claim Token** command \
This is a `POST` method that uses the Header variable: `Authorization` \
It calls the OAuth 2.0 `/access_token` endpoint to get the **Claim Token**. \
It uses `x-www-form-urlencoded` body. 

1. Click **Send** \
The `response` will contain a JSON payload. 
The JSON attribute `id_token` is the **Claim Token** and its value is saved
as the Postman environment variable `claimToken`.

1. Select the **4: Get RPT** command \
This is a `POST` method that uses the Header variable: `Authorization` \
It calls the OAuth 2.0 `/access_token` endpoint to get the **Requesting Party Token**. \
It uses `x-www-form-urlencoded` body. 

1. Click **Send** \
The `response` will contain a JSON payload. 
The JSON attribute `access_token` is the **Requesting Party Token** and 
its value is saved as the Postman environment variable `reqPartyToken`.

1. Select the **5: Re-Submit Request** command \
This is a `GET` method that uses the Header variable: \
`x-frdp-ssotoken` is set to the `RqPtokenId` value. \
`x-frdp-rpt` is set to the `reqPartyToken` value. \
The `resourceId` is in URL path. \
The `scopes` are set as a URL query parameter. \
Example: `https://{{FQDN}}/resource-server/rest/share/resources/{{resourceId}}/?scopes={{scopes}}`

1. Click **Send** \
The response is a JSON payload containing the objects, based on the specified `scopes`. \
`meta`: contains the resource's meta data (if specified as a scope) \
`content`: contains the resource's JSON data (if specified as a scope) \
`scopes`: information about the scopes from different perspectives \
`message`: status of the response \
`token`: the Requesting Party Token (RPT)

```json
{
    "meta": {
        "owner": "dcrane",
        "name": "IRA-456",
        "description": "Self managed IRS for Tom",
        "label": "Spouse IRA",
        "type": "finance-investment",
        "icon_uri": "https://img.icons8.com/doodle/48/000000/money.png"
    },
    "scopes": {
        "request": [ "meta", "content" ],
        "resource": [],
        "token": [ "meta", "content" ],
        "policy": [ "print", "meta", "content" ]
    },
    "message": "Success",
    "content": {
        "aNumber": 7,
        "aBoolean": true,
        "aString": "some string value",
        "array": [ "item1", "item2", "item3" ],
        "object": {
            "foo": "bar"
        }
    },
    "token": "Dpi-qH2NMMCVtff3oEZWQi3fHOE"
}
```

# Shared With Me

## NOTICE:  This use case is **not** part of the UMA 2.0 specification.  

This use case leverages the ForgeRock Access Manager APIs to obtain a collection of `resources` that have been "shared" to the **Requesting Party (RqP)** from any **Resource Owner (RO)**.

## Scenario

When the **Resource Owner (RO)** creates / registers a `resources` the UMA 2.0 registration process generates a `registrationId` and it is "wrapped" in a `resourceId` by the **Resource Servers (RS)**.  The `resourceId` is the "public" reference the the `resource`.  The UMA 2.0 Specification does not cover *how* the resource identifier is transferred the the **Requesting Parties (RqP)**.  This is call the Resource Transfer Of Information (RTOI) scenario.  

The **Requesting Party (RqP)** must know the *resource identifier* before they can initiate the resource access process.  The RTOI options can include the **Requesting Party (RqP)** pushing the *resource identifier* via a mechanism such as email or instant messaging.  Another RTOI option can involve the **Resource Owner (RO)** asking the **Resource Server (RS)** to what resources are currently being "Shared With Me".

## Procedure

1. The **Resource Owner (RO)** creates and shares a `resource`

1. The **Requesting Party (RqP)** gets a list of shared resources

## Testing Process

### RO: Create resource:

1. Setup Postman for the **Resource Owner (RO)**. \
Clear all cookies

1. Open the **UMA Resource Owner** Postman Collection

1. Open the **Authenticate** Folder *(login as the **Resource Owner (RO)**)*

1. Select the **Login** command \
Click **Send** 

1. Select the **Validate** command \
Click **Send** 

1. Open the **Manage Resource** Folder

1. Select the **Delete** command *(removes the existing `resource`, if it exists)* \
Click **Send**

1. Select the **Create** command \
Click **Send** 

1. Select the **Search** command \
Click **Send** 

1. Select the **Read** command \
(verify resource was created and registered.) \
Click **Send** 

### RqP: Shared With Me:

1. Setup Postman for the **Requesting Party (RqP)**. \
Clear all cookies

1. Open the **UMA Requesting Party** Postman Collection

1. Open the **Authenticate** Folder, *login as the **Requesting Party (RqP)***

1. Select the **Login** command \
Click **Send** 

1. Select the **Validate** command \
Click **Send** 

1. Open the **Extra** Folder

1. Select the **Shared With Me** command \
Click **Send** \
The response is a JSON structure.  The `results` JSON object contains details for each resource that has been shared with the **Requesting Party (RqP)**.  The `id` attribute is the *resource identifier* (`resource Id`), which can be used to issue a UMA 2.0 access request.

```json
{
    "quantity": 1,
    "results": [
        {
            "owner": "dcrane",
            "description": "Savings account with spouse",
            "label": "Joint Savings",
            "type": "finance-investment",
            "icon_uri": "https://img.icons8.com/doodle/48/000000/money.png",
            "name": "IRA-456",
            "scopes": [ "content", "meta", "print", "download" ],
            "id": "3caf168c-6dcd-413d-b739-59ea18e46530",
            "policy": [ "meta", "content" ]
        }
    ]
}
```

# Discovery

## NOTICE:  This use case is **not** part of the UMA 2.0 specification.  

This use case leverages "value add" features of the **Resource Server (RS)** and *meta data* that is associated with resources.

## Scenario

The **Shared With Me** scenario (above) relies on the resource being UMA 2.0 *registered* and having a *policy* giving the **Requesting Party (RqP)** access.  The **Resource Server (RS)** supports the ability of creating and registering `resources` without having a *policy*.  

A **Resource Owner (RO)** may create / register resources but not pre-register or pre-assign a policy to them.  The **RO** might not know, in advance, which **Requesting Parties (RqP)** want or need access.  The **RO** would like to allow any **RqP** to "browse" or "discover" some or all of their resources.  The **Resource Server (RS)** provides a *discover* service where the **RqP** can get a list of `resources` that the **RO** has chosen to make *discoverable*.  The **RqP** can use the `resourceId` from the response to initiate a Request For Access.

## Procedure

1. The **Resource Owner (RO)** creates a discoverable resource

1. The **Requesting Party (RqP)** gets a list of discoverable resources

## Testing Process

### RO: Create resource:

1. Setup Postman for the **Resource Owner (RO)**. \
Clear all cookies

1. Open the **UMA Resource Owner** Postman Collection

1. Open the **Authenticate** Folder *(login as the **Resource Owner (RO)**)*

1. Select the **Login** command \
Click **Send** 

1. Select the **Validate** command \
Click **Send** 

1. Open the **Manage Resource** Folder

1. Select the **Delete** command *(removes the existing `resource`, if it exists)* \
Click **Send**

1. Select the **Create** command

1. Select the **Body** tab

1. Edit the body of the Request \
In the `meta` object, change the `discoverable` attribute's value to "true" ... `"discoverable": true,` \
Remove the `policy` object that is inside the `register` object \
(A resource **MUST** be registered to enable *discovery*, thus needs the `register` object) \
Click **Send** 

1. Select the **Search** command \
(sets the PostMan Environment Variable for the `resourceId`) \
Click **Send** 

1. Select the **Read** command \
Click **Send** \
(verify resource was created, not registered, and that the `meta` attribute `discoverable` is set to "true")

### RqP: Discover Resource:

1. Setup Postman for the **Requesting Party (RqP)**. \
Clear all cookies

1. Open the **UMA Requesting Party** Postman Collection

1. Open the **Authenticate** Folder *(login as the **Requesting Party (RqP)**)*

1. Select the **Login** command \
Click **Send** 

1. Select the **Validate** command \
Click **Send** 

1. Open the **Extra** Folder \

1. Select the **Discover Resources** command \
Click **Send**
The response is a JSON structure.  The `results` JSON object contains details for each resource that has been made *"discoverable"* by the **Resource Owner (RO)**. The `id` attribute is the *resource identifier* (`resource Id`), which can be used to issue a UMA 2.0 access request.

```json
{
    "quantity": 1,
    "results": [
        {
            "owner": "dcrane",
            "name": "IRA-456",
            "description": "Self managed IRA for Tom",
            "id": "89ac3ed6-ba80-493c-b118-0a4b27cab102",
            "label": "Spouse IRA",
            "scopes": [ "content", "meta", "print", "download" ],
            "type": "finance-investment",
            "icon_uri": "https://img.icons8.com/doodle/48/000000/money-bag--v1.png"
        }
    ]
}
```

# Revoke Access

## NOTICE:  This use case is **not** part of the UMA 2.0 specification.  

This use case leverages the ForgeRock Access Manager APIs to modify a `policy`, for a given `resource`, and remove access for the **Requesting Party (RqP)**.

## Scenario

A **Requesting Party (RqP)** may have access to a `resource` that they no longer want.  The **RqP** may not want to be "associated" with the `resource` any longer for liability / legal reasons.  Traditionally, only the **Resource Owner (RO)** can change a `resource` policy to remove a **RqP's** access.  

The **Resource Server (RS)** provides a *revoke* service where a **Requesting Party (RqP)** can remove their own access to a `resource`.

## Procedure

1. The **Resource Owner (RO)** creates resource

1. The **Requesting Party (RqP)** revokes access

1. The **Resource Owner (RO)** verifies policy

## Testing Process

### RO: Create resource:

1. Setup Postman for the **Resource Owner (RO)**. \
Clear all cookies

1. Open the **UMA Resource Owner** Postman Collection

1. Open the **Authenticate** Folder *(login as the **Resource Owner (RO)**)*

1. Select the **Login** command \
Click **Send** 

1. Select the **Validate** command \
Click **Send** 

1. Open the **Manage Resource** Folder

1. Select the **Delete** command *(removes the existing `resource`, if it exists)* \
Click **Send**

1. Select the **Create** command \
Click **Send** 

1. Select the **Search** command \
(sets the PostMan Environment Variable for the `resourceId`) \
Click **Send** 

1. Select the **Read** command \
Click **Send** \
(verify resource was created, not registered, and that the `meta` attribute `discoverable` is set to "true")

### RqP: Revokes Access

1. Setup Postman for the **Requesting Party (RqP)**. \
Clear all cookies

1. Open the **UMA Requesting Party** Postman Collection

1. Open the **Authenticate** Folder *(login as the **Requesting Party (RqP)**)*

1. Select the **Login** command \
Click **Send** 

1. Select the **Validate** command \
Click **Send** 

1. Open the **Extra** Folder 

1. Select the **Revoke My Access** command \
Click **Send** 

### RO: Verify policy

1. Setup Postman for the **Resource Owner (RO)**. \
Clear all cookies

1. Open the **UMA Resource Owner** Postman Collection

1. Open the **Authenticate** Folder *(login as the **Resource Owner (RO)**)*

1. Select the **Login** command \
Click **Send** 

1. Select the **Validate** command \
Click **Send** 

1. Open the **Extra** Folder

1. Select the **Register Read** command \
Click **Send** \
The **Requesting Party (RqP)** should no longer existing in the JSON Array `permissions`, as a JSON Object, with a `subject` attribute ...

```json
{
    "permissions": [
        {
            "subject": "myoshida",
            "scopes": [
                "content"
            ]
        },
        {
            "subject": "aadams",
            "scopes": [
                "meta",
                "content"
            ]
        }
    ]
}
```

