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

Set Postman Preferences:

- General:\
Disable SSL certificate verification

Load the Postman Collections: 
- `UMA_RO.postman_collection.json`

Set Postman Environment variables for the **Resource Server** ... NOTE: set both the **`INITIAL VALUE`** and **`CURRENT VALUE`** the same.
- `FQDN-openam`:\
Set your **Authorization Server (AS)** deployment, *ForgeRock Access Manager*\
Example: `https://uma.example.com:443/openam`
- `FQDN-rs`:\
Set your **Resource Server (RS)** deployment\
Example: `https://uma.example.com:443/resource-server`
- `userName`:\
Set this to `dcrane`
- `tokenId`:\
Clear the value

Remove **all** the Cookies

# Overview

The **Resource Owner (RO)** is a user that owns a `resource` and wants to *share* it with another user, the **Requesting Party (RqP)**.

A `resource` can represent anything.  This project uses a *document* concept to simulate a `resource` that is being shared.

The **Resource Server (RS)** is designed to be flexible and supports a `resource` life-cycle with a number of states.  

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
to authenticate the user **(RO)**
1. Click **Send** \
A JSON payload is returned: \
```{"tokenId":"...","successUrl":"/openam/console","realm":"/"}```\
The `tokenId` attribute is saved as a Postman environment variable. \
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

1. Open the **UMA Resource Owner** Postman Collection
1. Open the **Manage Resource** Folder
1. Select the **Create** command\
This is a `POST` method that uses Header variables: `X-frdp-ssotoken` that is set to the `tokenId` value
1. Click **Send** \
The `resource` is created in the **Resource Server (RS)**.\
The `content` was added to the **Content Server (CS)**.\
The `resource` was registered with the **Authorization Server (AS)**.\
The `policy` was applied with the **Authorization Server (AS)**.

# Search

Run the *search* command to get a collection of resources.  You **must** run this operation, the first element in the collection is used for the other operations.

1. Open the **UMA Resource Owner** Postman Collection
1. Open the **Manage Resource** Folder
1. Select the **Search** command\
This is a `GET` method that uses Header variables: `X-frdp-ssotoken` that is set to the `tokenId` value
1. Click **Send**\
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

1. Open the **UMA Resource Owner** Postman Collection
1. Open the **Manage Resource** Folder
1. Select the **Read** command\
This is a `GET` method that uses Header variables: `X-frdp-ssotoken` that is set to the `tokenId` value
1. Click **Send**\
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

1. Open the **UMA Resource Owner** Postman Collection
1. Open the **Manage Resource** Folder
1. Select the **Delete** command\
This is a `DELETE` method that uses Header variables: `X-frdp-ssotoken` that is set to the `tokenId` value
1. Click **Send**


