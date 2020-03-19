# UMA Resource Owner

This document covers how to test **UMA Resource Owner (RO)** operations using the **Resource Server (RS)** 

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

Load Environment Variables:
- `../Resource_Server.postman_environment.json`

Load Collection: 
- `UMA_RO.postman_collection.json`

Set Environment variables for the **Resource Server** ... NOTE: set both the **`INITIAL VALUE`** and **`CURRENT VALUE`** the same.
- `FQDN-openam`:\
Set your **Authorization Server (AS)** deployment URI, *ForgeRock Access Manager*\
Example: `https://uma.example.com:443/openam`
- `FQDN-rs`:\
Set your **Resource Server (RS)** deployment URI\
Example: `https://uma.example.com:443/resource-server`

Remove **all** the Cookies

# Overview

The **Resource Owner (RO)** is a user that owns a `resource` and wants to *share* it with another user, the **Requesting Party (RqP)**.

A `resource` can represent anything.  This project uses a *document* concept to simulate a `resource` that is being shared.

The **Resource Server (RS)** is designed to be flexible and supports the `resource` life-cycle states:

| State | Description |
| ----- | ----------- |
| Created | The `resource` is only known to the **Resource Server(RS)**.  It only has meta data.
| Content | *(optional)* The `resource` has an associated external JSON document.  The **RS** manages a handle to it
| Registered | The `resource` has been *registered* with the **Authorization Server (AS)**.  The registered `resource` is managed by the **RS**.
| Policy | The registered `resource` has at least one policy (permission) assigned, allowing one or more users to access the `resource` using specific scopes.

Each of the above `resource` states can be managed individually.

# Authenticate

The **Resource Owner (RO)** needs to authenticate to the **Authorization Server (AS)**, *ForgeRock Access Manager*, and obtain a Single-Sign-On (SSO) token: `tokenId`

1. Open the **UMA Resource Owner** Postman Collection

1. Open the **Authenticate** Folder

1. Select the **Login** command \
This is a `POST` method that uses Header variables: `X-OpenAM-Username` and `X-OpenAM-Password` 
to authenticate the **RO** user

1. Click **Send** \
A JSON payload is returned: \
```{"tokenId":"...","successUrl":"/openam/console","realm":"/"}```\
The `tokenId` attribute is saved as a Postman environment variable: `ROtokenId`. \
The **Authorization Server (AS)**, *ForgeRock Access Manager*, sets a Cookie: `iPlanetDirectoryPro`

1. Select the **Validate** command \
This is a `POST` method that uses the Cookie `iPlanetDirectoryPro` and validates that the user's session is still valid.

1. Click **Send** \
A valid session will return a JSON payload: \
```{"valid":true,"sessionUid":"...","uid":"dcrane","realm":"/"}```\
The `uid` value **MUST** be `dcrane`, the **Resource Owner (RO)**

1. DO NOT CONTINUE WITHOUT A VALID SSO SESSION

# Create 

A `resource` can be created by explicitly executing the REST/JSON interface for each state.  The create process also supports a one-step procedure for all states.  This example will cover setting all states in one process.

1. Open the **`UMA Resource Owner`** Postman Collection

1. Open the **`Resource`** Folder

1. Select the **`Create content-default`** command\
This is a `POST` method that uses Header variables: `X-frdp-ssotoken` that is set to the `ROtokenId` value

1. Click **`Send`** \
The `resource` is created in the **Resource Server (RS)**.\
The `content` was added to the **Content Server (CS)**.\
The `resource` was registered with the **Authorization Server (AS)**.\
The `policy` was applied with the **Authorization Server (AS)**.

# Search

Run the *search* command to get a collection of resources.  You **must** run this operation, the first element in the collection is used for the other operations.

1. Open the **`UMA Resource Owner`** Postman Collection

1. Open the **`Resource`** Folder

1. Select the **`Search`** command\
This is a `GET` method that uses Header variables: `X-frdp-ssotoken` that is set to the `ROtokenId` value

1. Click **`Send`**\
The response is a JSON payload that contains an array of `resource` identifiers.\
The first item in the array `results` will be used for other commands.

```json
{
    "quantity": 1,
    "results": ["08d7b8bc-5f20-4b29-b11d-eb3132966954"]
}
```

# Read

Run the *read* command to get a specific resource.

1. Open the **`UMA Resource Owner`** Postman Collection

1. Open the **`Resource`** Folder

1. Select the **`Read`** command\
This is a `GET` method that uses Header variables: `X-frdp-ssotoken` that is set to the `ROtokenId` value

1. Click **`Send`**\
The response is a JSON payload that contains all the objects for the `resource`

```json
{
    "owner": "dcrane",
    "access": "shared",
    "meta": {
        "discoverable": false,
        "name": "IRA-456",
        "description": "Self managed IRA for Tom",
        "label": "Spouse IRA",
        "type": "finance-investment"
    },
    "content": {
        "aNumber": 7,
        "aBoolean": true,
        "aString": "some string value",
        "array": [ "item1", "item2", "item3" ],
        "object": {
            "foo": "bar"
        }
    },
    "register": {
        "resource_scopes": [ "content", "meta", "print", "download" ],
        "icon_uri": "https://img.icons8.com/doodle/48/000000/money.png",
        "policy": {
            "permissions": [
                {
                    "subject": "bjensen",
                    "scopes": [ "print", "meta", "content" ]
                },
                {
                    "subject": "myoshida",
                    "scopes": [ "content" ]
                },
                {
                    "subject": "aadams",
                    "scopes": [ "meta", "content" ]
                }
            ]
        }
    }
}
```

# Delete

Run the *delete* command to remove the resource. The following steps will be completed:
- Remove the `policy` (permissions)
- De-register the `resource`
- Delete the `content`, from the **Content Server (CS)**
- Delete the `resource`, from the **Resource Server (RS)**

1. Open the **`UMA Resource Owner`** Postman Collection

1. Open the **`Resource`** Folder

1. Select the **`Delete`** command\
This is a `DELETE` method that uses Header variables: `X-frdp-ssotoken` that is set to the `ROtokenId` value

1. Click **`Send`**

# People

## NOTICE:  This use case is **not** part of the UMA 2.0 specification.  

This use case leverages the ForgeRock Access Manager APIs for obtaining information about which "People", **Requesting Parties (RqP)**, have access to resources that are owned by a given **Resource Owner (RO)**.

## Scenario

The **Resource Owner (RO)** will typically have many `resources` shared with multiple *People* / **Requesting Parties (RqP)**.  The `resource` search functionality of the **Resource Server (RS)** returns a list of resources that the **RO** owns.  It would be nice if the **RO** could get a list of *People* that have access to their resources.

The **RS** provides a "value add" service, not part of the UMA 2.0 specification, that enables the **RO** to get a list of "People" that have access to resources.  The **RS** leverages the ForgeRock Access Manager APIs to access policies associated to registered UMA `resources` and return a collection of *People* (the **RqP**) along with what `resources` and `scopes` are assigned to each *Person*.

## Procedure

1. The **Resource Owner (RO)** creates and shares a `resource`

1. The **Resource Owner (RO)** accesses the `subjects` endpoint

## Testing Process

1. Setup Postman for the **Resource Owner (RO)**. \
Clear all cookies

1. Open the **UMA Resource Owner** Postman Collection

1. Open the **Authenticate** Folder *(login as the **Resource Owner (RO)**)*

1. Select the **Login** command \
Click **Send** 

1. Select the **Validate** command \
Click **Send** 

1. Open the **Extra** Folder

1. Select the **Subjects Search** command \
Click **Send** \
The response is a JSON payload containing a collection of *People* that have access to one or more `resources` ...

```json
{
    "quantity": 3,
    "results": [
        {
            "subject": "aadams",
            "resources": [
                {
                    "name": "IRA-456",
                    "scopes": [ "meta", "content" ],
                    "id": "3caf168c-6dcd-413d-b739-59ea18e46530"
                }
            ]
        },
        {
            "subject": "bjensen",
            "resources": [
                {
                    "name": "IRA-456",
                    "scopes": [ "meta", "content" ],
                    "id": "3caf168c-6dcd-413d-b739-59ea18e46530"
                }
            ]
        },
        {
            "subject": "myoshida",
            "resources": [
                {
                    "name": "IRA-456",
                    "scopes": [ "meta", "content" ],
                    "id": "3caf168c-6dcd-413d-b739-59ea18e46530"
                }
            ]
        }
    ]
}
```

# Requests

## NOTICE:  This use case is **not** part of the UMA 2.0 specification.  

This use case leverages the ForgeRock Access Manager APIs for processing UMA access requests.

## Scenario

You may have a situation where the **Resource Owner (RO)** creates and registers a UMA resource.  But, the `resource` has not been shared with the **Requesting Party(RqP)**.  The **RqP** follows the [UMA flow](../Rqp/README.md) to access the `resource` ... the protocol step, to get the **Requesting Party Token (RPT)**, will fail with an error message.  The message indicates that a request has been submitted.

When the **Authorization Server (AS)**, ForgeRock Access Manager, is unable to create a **Requesting Party Token (RPT)** ... because the **RO** has not configured a *policy* which grants access to the **RqP** with the specified `scopes` for the specific `resource` ... the ForgeRock Access Manager will create an access request for the **RO**.  The **RO** can either `allow` or `deny` the access request for the `resource`.  If the **RO** allows the request, the ForgeRock Access Manager will create a policy granting access to the **RqP** for the `resource` with the specified `scopes`.

## Procedure

1. The **Resource Owner (RO)** creates a `resource` without a policy.

1. The **Requesting Party (RqP)** attempts to access the `resources`.  An access `request` is generated 

1. The **Resource Owner (RO)** reviews and approves the `request`.

1. The **Requesting Party (RqP)** re-attempts to access the `resource`.

## Testing Process

### RO: Create resource:

1. Setup Postman for the **Resource Owner (RO)**. \
Clear all cookies

1. Open the **UMA Resource Owner** Postman Collection

1. Open the **Authenticate** Folder, *login as the **Resource Owner (RO)***

1. Select the **Login** command \
Click **Send** 

1. Select the **Validate** command \
Click **Send** 

1. Open the **Resource** Folder

1. Select the **Delete** command *(removes the existing `resource`, if it exists)* \
Click **Send**

1. Select the **Create** command

1. Select the **Body** tab

1. Edit the body of the Request \
Remove the the `policy` object from the JSON structure \
Click **Send** 

1. Select the **Search** command \
(find the new resource and set Postman variable) \
Click **Send** 

1. Select the **Read** command \
(verify resource was created and registered but has **no** policy.) \
Click **Send** 

### RqP: Attempt access:

1. Setup Postman for the **Requesting Party (RqP)**. \
Clear all cookies

1. Open the **UMA Requesting Party** Postman Collection

1. Open the **Authenticate** Folder, *login as the **Requesting Party (RqP)***

1. Select the **Login** command \
Click **Send** 

1. Select the **Validate** command \
Click **Send** 

1. Open the **Get Resource** Folder

1. Select the **1: Submit Request** command \
Click **Send** 

1. Select the **2: Get Authz Code** command \
Click **Send**

1. Select the **3: Get Claim Token** command \
Click **Send** 

1. Select the **4: Get RPT** command \
Click **Send** \
The request will fail, `403 Forbidden`, because the **Requesting Party (RqP)** does not have permission (a policy). \
An "access request" is sent to the **Resource Owner (RO)**

```json
{
    "ticket": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJodHRwczovL2lkcC5mcmRwY2xvdWQuY29tOjQ0My9vcGVuYW0vb2F1dGgyIiwiaXNzIjoiaHR0cHM6Ly9pZHAuZnJkcGNsb3VkLmNvbTo0NDMvb3BlbmFtL29hdXRoMiIsIml0IjoxLCJleHAiOjE1Nzk3MTc3MjksInRpZCI6ImJlY2MyZTcxLTQ3ZWMtNDBjYS1hMTFlLWZjNmI2NzNjMTMxNDIiLCJmb3JnZXJvY2siOnsic2lnIjoiWnVBJVldMVIjaUkwdHVeJkFTMDA6cVcxZ0YzJEIoXWNAU2orfXktPiJ9fQ.lEedNWvoymzfu6T2o6nqtEUpTIj9zKw9FgZL8IwZRIE",
    "error_description": "The client is not authorised to access the requested resource set. A request has been submitted to the resource owner requesting access to the resource",
    "error": "request_submitted"
}
```

### RO: Approve request

1. Setup Postman for the **Resource Owner (RO)**. \
Clear all cookies

1. Open the **UMA Resource Owner** Postman Collection

1. Open the **Authenticate** Folder, *login as the **Resource Owner (RO)***

1. Select the **Login** command \
Click **Send** 

1. Select the **Validate** command \
Click **Send** 

1. Open the **Pending Requests** Folder

1. Select the **Request Search** command \
Click **Send** \
There should be one `requestId` in the JSON array. \
The first `requestId` is saved as a Postman Environment Variable

1. Select the ** Request Read** command \
Click **Send** \
The details of the request are returned as JSON ... \
`{"resource":"IRA-456","permissions":["meta","content"],"_id":"ac0bb370-c1b9-4a09-aeef-3d0a78b410a20","user":"bjensen","when":1579717632556}`

1. Select the **Request Approve Deny** command \
Click **Send** \
The body contains JSON which approves the request: `{"action": "approve"}` \
The request is approved, a policy is created / updated to grant the **Requesting Party (RqP)** access to the `resource` for the specified `scopes`

### RqP: Re-Attempt access:

1. Setup Postman for the **Requesting Party (RqP)**. \
Clear all cookies

1. Open the **UMA Requesting Party** Postman Collection

1. Open the **Authenticate** Folder, *login as the **Requesting Party (RqP)***

1. Select the **Login** command \
Click **Send** 

1. Select the **Validate** command \
Click **Send** 

1. Open the **Get Resource** Folder

1. Select the **1: Submit Request** command \
Click **Send** 

1. Select the **2: Get Authz Code** command \
Click **Send**

1. Select the **3: Get Claim Token** command \
Click **Send** 

1. Select the **4: Get RPT** command \
Click **Send** \
The **Requesting Party Token (RPT)** is now issued because the request approved.

1. Select the **5: Re-Submit Request** command \
Click **Send** \
The `resource` is returned.

