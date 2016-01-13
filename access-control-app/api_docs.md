## API Documentation

***

See the [CMR Data Partner User Guide](https://wiki.earthdata.nasa.gov/display/CMR/CMR+Data+Partner+User+Guide) for a general guide to utilizing the CMR Ingest API as a data partner.
See the [CMR Client Partner User Guide](https://wiki.earthdata.nasa.gov/display/CMR/CMR+Client+Partner+User+Guide) for a general guide to developing a CMR client.
Join the [CMR Client Developer Forum](https://wiki.earthdata.nasa.gov/display/CMR/CMR+Client+Developer+Forum) to ask questions, make suggestions and discuss topics like future CMR capabilities.

### Access Control API Overview

  * [/groups](#groups)
    * [POST - Create Group](#create-group)

***

## <a name="api-conventions"></a> API Conventions

This defines conventions used across the Access Control API.

### <a name="headers"></a> Headers

This defines common headers on the Access Control API.

#### <a name="content-type-header"></a> Content-Type Header

Content-Type is a standard HTTP header that specifies the content type of the body of the request. Access Control only supports `application/json` as the representation type for data.

#### <a name="echo-token-header"></a> Echo-Token Header

All Access Control API operations require specifying a token obtained from URS or ECHO. The token should be specified using the `Echo-Token` header.

***

### <a name="responses"></a> Responses

### <a name="response-headers"></a> Response Headers

#### <a name="CMR-Request-Id-header"></a> cmr-request-id

This header returns the unique id assigned to the request. This can be used to help debug client errors. The value is a long string of the form

    828ef0b8-a876-4579-85db-3cc9d1b5f6e5

#### <a name="http-status-codes"></a> HTTP Status Codes

| Status Code | Description                                                                                                                                |
|-------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| 200         | Success                                                                                                                                    |
| 400         | Bad request. The body will contain errors.                                                                                                 |
| 404         | Not found. This could be returned either because the URL isn't known by access control or the item wasn't found.                           |
| 409         | Conflict. This is returned when a revision id conflict occurred while saving the item.                                                     |
| 415         | Unsupported Media Type. The body will return an error message that contains the list of supported formats.                                 |
| 422         | Unprocessable entity. Access control understood the request, but the concept failed ingest validation rules. The body will contain errors. |
| 500         | Internal error. Contact CMR Operations if this occurs.                                                                                     |
| 503         | Internal error because a service dependency is not available.                                                                              |

#### <a name="successful-responses"></a> Successful Responses

Successful responses will return an HTTP Status code of 200 and a body containing the CMR Concept Id of the item that was updated or deleted along with the revision id.

    {"concept-id":"AG12345-PROV","revision-id":1}

## <a name="groups"></a> Groups

Groups are used to identify sets of users for the assignment of access privileges. Groups are either owned by a provider or the system. Provider level groups define sets of users access to that providers data. System level groups define sets of users for assigning system level access. Groups have the following fields:

* `name` - Required field that uniquely identifies a system group or a group within a provider.
* `provider-id` - Id of the provider that owns the group. If this isn't present then the group will be a system level group.
* `description` - Required field that describes the group.
* `legacy-guid` - Internal use only. This is used for ECHO Kernel interoperability.

### <a name="create-group"></a> Create Group

Tags are created by POSTing a JSON representation of a group to `%CMR-ENDPOINT%/groups` along with a valid ECHO token. The response will contain a concept id identifying the group along with the group revision id.

```
curl -XPOST -i -H "Content-Type: application/json" -H "Echo-Token: XXXXX" %CMR-ENDPOINT%/groups -d \
'{
  "name": "Administrators",
  "description": "The group of users that manages PROV1s data holdings."
 }'

HTTP/1.1 200 OK
Content-Type: application/json;charset=ISO-8859-1
Content-Length: 48

{"revision-id":1,"concept-id":"AG1200000000-CMR"}
```