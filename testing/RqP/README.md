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

Set Postman Preferences:

- General:\
Disable SSL certificate verification

Load the Postman Collections: 
- `UMA_RqP.postman_collection.json`

Set Postman Environment variables for the **Resource Server** ... NOTE: set both the **`INITIAL VALUE`** and **`CURRENT VALUE`** the same.
- `FQDN-openam`:\
Set your **Authorization Server (AS)** deployment, *ForgeRock Access Manager*\
Example: `https://uma.example.com:443/openam`
- `FQDN-rs`:\
Set your **Resource Server (RS)** deployment\
Example: `https://uma.example.com:443/resource-server`
- `userName`:\
Set this to `bjensen`
- `tokenId`:\
Clear the value

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
The `tokenId` attribute is saved as a Postman environment variable. \
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
This is a `GET` method that uses the Header variable: `x-frdp-ssotoken` set to the `tokenId` value. \
The `resourceId` is in URL path. \
The `scopes` are set as a URL query parameter. \
Example: `https://{{FQDN}}/resource-server/rest/share/resources/{{resourceId}}/?scopes={{scopes}}`

1. Click **Send** \
The `request` will fail, due to a missing (or invalid) **Requesting Party Token (RPT)** \
The `response` will contain a JSON payload.  The **Permission Ticket**, (`ticket` attribute) and the **AS URI** (`as_uri` attribute) will be saved as Postman environment variables.

1. Select the **2: Get Authz Code** command \
This is a `POST` method that uses the Header variable: `iPlanetDirctoryPro` set to the `tokenId` value. \
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
`x-frdp-ssotoken` is set to the `tokenId` value. \
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
        "request": [
            "meta",
            "content"
        ],
        "resource": [],
        "token": [
            "meta",
            "content"
        ],
        "policy": [
            "print",
            "meta",
            "content"
        ]
    },
    "message": "Success",
    "content": {
        "aNumber": 7,
        "aBoolean": true,
        "aString": "some string value",
        "array": [
            "item1",
            "item2",
            "item3"
        ],
        "object": {
            "foo": "bar"
        }
    },
    "token": "Dpi-qH2NMMCVtff3oEZWQi3fHOE"
}
```
