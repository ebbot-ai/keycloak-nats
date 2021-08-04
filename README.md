# keycloak-nats

A Keycloak event listener which publishes events to NATS or NATS Streaming (STAN)

## Inspiration

The layout of this project is inspired
by [aznamier/keycloak-event-listener-rabbitmq](https://github.com/aznamier/keycloak-event-listener-rabbitmq).

## Installation

1. Download the source code (for example via Git)
2. Build the project (`./gradlew shadowJar`)
3. Move the built jar (located in `build/libs` and ends with `-all.jar`) into
   the `$KEYCLOAK_HOME/standalone/deployments` directory
4. Keycloak supports hot reload, however, you **may** need to restart it
5. Log into the admin console, select the realm you want to enable NATS event adapting for, head over to 'Events >
   Config' and add `keycloak-nats-adapter` to the list of active event listeners

## Configuration

Configuration is done via environment variables. I read something about some included XML configuration solution and
decided not to use it because we don't live in the stone age anymore.

| Environment Variable                 | Data Type | Description                                                               | Default Value           |
|--------------------------------------|-----------|---------------------------------------------------------------------------|-------------------------|
| `KEYCLOAK_NATS_STREAMING`            | boolean   | Whether or not to use NATS Streaming (STAN); plain NATS is used otherwise | `false`                 |
| `KEYCLOAK_NATS_URL`                  | string    | The NATS URL to connect to; may contain authentication details            | `nats://localhost:4222` |
| `KEYCLOAK_NATS_STREAMING_CLUSTER_ID` | string    | The cluster ID to use when NATS Streaming (STAN) is used                  | `<empty>`               |
| `KEYCLOAK_NATS_STREAMING_CLIENT_ID`  | string    | The client ID to use when NATS streaming (STAN) is used                   | `<empty>`               |

## Channel Name / Subject

The channel name or subject structure depends on the event type:

* **Admin Event:** `keycloak.event.admin.<realm>.<success/error>.<resourceType>.<operation>` (for
  example `keycloak.event.admin.master.success.user.update`)
* **Client Event:** `keycloak.event.client.<realm>.<success/error>.<clientId>.<type>` (for
  example `keycloak.event.client.master.success.security-admin-console.refresh_token`)

The `resourceType`, `operation` and `type` parts are **always forced to be lowercase** even though Keycloak fires them
in an uppercase format.

Spaces get replaced with underscores.

## Representation Examples

### Admin Event

Channel `keycloak.event.admin.mesy.success.user.update`

```json
{
  "id": "5ee1b9ee-426d-4f69-9877-b3a96b54da35",
  "time": 1628089918834,
  "realmId": "mesy",
  "authDetails": {
    "realmId": "master",
    "clientId": "56f3e1c8-1a94-4373-9173-90ed06ee9c83",
    "userId": "96087d0a-8334-4305-be07-72166ca937a6",
    "ipAddress": "172.30.0.1"
  },
  "resourceType": "USER",
  "operationType": "UPDATE",
  "resourcePath": "users/db85bef5-f1b8-462d-a563-7de86cf7a2da",
  "representation": "{\"id\":\"db85bef5-f1b8-462d-a563-7de86cf7a2da\",\"createdTimestamp\":1628088891982,\"username\":\"johnny\",\"enabled\":true,\"totp\":false,\"emailVerified\":false,\"firstName\":\"john\",\"lastName\":\"doe\",\"email\":\"john@doe.com\",\"attributes\":{},\"disableableCredentialTypes\":[],\"requiredActions\":[],\"notBefore\":0,\"access\":{\"manageGroupMembership\":true,\"view\":true,\"mapRoles\":true,\"impersonate\":true,\"manage\":true}}",
  "error": null,
  "resourceTypeAsString": "USER"
}
```

### Client Event

Channel `keycloak.event.client.mesy.success.account-console.update_password`

```json
{
  "id": "a1e97bf5-f1ac-477e-beb4-e8be6775f857",
  "time": 1628091019154,
  "type": "UPDATE_PASSWORD",
  "realmId": "mesy",
  "clientId": "account-console",
  "userId": "db85bef5-f1b8-462d-a563-7de86cf7a2da",
  "sessionId": null,
  "ipAddress": "172.30.0.1",
  "error": null,
  "details": {
    "auth_method": "openid-connect",
    "custom_required_action": "UPDATE_PASSWORD",
    "response_type": "code",
    "redirect_uri": "http://localhost:8080/auth/realms/mesy/account/?referrer=security-admin-console&referrer_uri=http%3A%2F%2Flocalhost%3A8080%2Fauth%2Fadmin%2Fmaster%2Fconsole%2F%23%2Frealms%2Fmesy%2Fusers%2Fdb85bef5-f1b8-462d-a563-7de86cf7a2da%2Fuser-credentials#/security/signingin",
    "remember_me": "false",
    "code_id": "594a0a98-1889-45c9-a6b9-58b731cfa13f",
    "response_mode": "fragment",
    "username": "johnny"
  }
}
```
