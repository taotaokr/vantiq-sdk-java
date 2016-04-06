# API Reference

The Vantiq API provide an API for interacting with the Vantiq server.  Each
API returns a [Promise](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise) that provides the results or errors from the API call.

This document defines the Vantiq Client SDK.  Please refer to the [Vantiq Reference Guides](https://dev.vantiq.com/docs/api/developer.html) for details on the how to use the Vantiq system.

## Vantiq API

The Vantiq object provides the main API for the Vantiq Java SDK.  This SDK follows the Vantiq REST API resources model.

### Resources

Each of the SDK API methods corresponds to a specific REST API operation on a specific resource.  For example,
`select` performs a query against a given resource.  `select("types", ...)` queries against defined data types.
`select("procedures", ...)` queries against defined procedures.

The available system resources are defined in the `Vantiq.SystemResources` enum and include the following:

Enum Name  | Resource Name  | Type Name    | Description
---------- | -------------- | ------------ | -----------
DOCUMENTS  | documents      | ArsDocument  | Unstructured documents stored in the Vantiq system
NAMESPACES | namespaces     | ArsNamespace | Namespaces defined in the Vantiq system
NODES      | nodes          | ArsPeerNode  | Node defined in the Vantiq system to support federation
PROFILES   | profiles       | ArsProfile   | Vantiq user permission profiles
PROCEDURES | procedures     | ArsComponent | Procedures defined in the Vantiq system
RULES      | rules          | ArsRuleSet   | Rules defined in the Vantiq system
SCALARS    | scalars        | ArsScalar    | User-defined property type definitions
SOURCES    | sources        | ArsSource    | Data sources defined in the Vantiq system
TOPICS     | topics         | ArsTopic     | User-defined topics in the Vantiq system
TYPES      | types          | ArsType      | Data types defined in the Vantiq system
USERS      | users          | ArsUser      | Vantiq user accounts

Data types defined in the Vantiq system can also be used as resources.  For example, if you define data
type `MyNewType`, then `MyNewType` is now a legal resource name that can be used in the API methods.

### API

* [io.vantiq.client.Vantiq](#Vantiq)
    * [authenticate](#Vantiq-authenticate)
    * [select](#Vantiq-select)
    * [selectOne](#Vantiq-selectOne)
    * [count](#Vantiq-count)
    * [insert](#Vantiq-insert)
    * [update](#Vantiq-update)
    * [upsert](#Vantiq-upsert)
    * [delete](#Vantiq-delete)
    * [deleteOne](#Vantiq-deleteOne)
    * [publish](#Vantiq-publish)
    * [execute](#Vantiq-execute)
    * [query](#Vantiq-query)

### ResponseHandler

* [ResponseHandler](#ResponseHandler)

### Error

* [VantiqError](#VantiqError)

## <a id="vantiq"></a> Vantiq

The `Vantiq` constructor creates a new instance of the `Vantiq` SDK object.
The SDK expects that the first operation is to authenticate onto the
specified Vantiq server.  After successfully authenticated, the client
is free to issue any requests to the Vantiq server.

This class exposes the [Vantiq RESTful API](https://dev.vantiq.com/docs/api/developer.html#api-reference-guide)

### Signature

```java
Vantiq vantiq = new Vantiq(String server[, int apiVersion])
```

### Option Parameters

Name | Type | Required | Description
:--: | :--: | :------:| -----------
server | String | Yes | The Vantiq server URL to connect to, e.g. `https://dev.vantiq.com`
apiVersion | int | No | The version of the API to use.  Defaults to the latest.

### Returns

An instance of the `Vantiq` object

### Example

```java
Vantiq vantiq = new Vantiq("https://dev.vantiq.com");
```

## <a id="vantiq-authenticate"></a> Vantiq.authenticate

The `authenticate` method connects to the Vantiq server with the given 
authentication credentials used to authorize the user.  Upon success,
an access token is provided to the client for use in subsequent API 
calls to the Vantiq server.  The username and password credentials
are not stored.

### Signature

```java
void vantiq.authenticate(String username, String password, ResponseHandler handler)
```

### Parameters

Name | Type | Required | Description
:--: | :--: | :------:| -----------
username | String | Yes | The username to provide access to the Vantiq server
password | String | Yes | The password to provide access to the Vantiq server
handler | ResponseHandler | Yes | Listener that is called upon success or failure

### Returns

The `authenticate` method calls `handler.onSuccess` with a Boolean body indicating if the authentication was successful.  Upon any failure, including invalid credentials, `handler.onError` is called.  In the case
of invalid credentials, `handler.onError` is called and the HTTP status code is *401*.

### Example

```java
vantiq.authenticate("joe@user", "my-secr3t-passw0rd!#!", new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("Authenticated!");
    }
    
    @Override public void onError(List<VantiqError> errors, Response response) {
        super.onError(errors, response);
        System.out.println("Errors: " + errors);
    }

});
```

## <a id="vantiq-select"></a> Vantiq.select

The `select` method issues a query to select all matching records for a given 
resource.

### Signature

```java
void vantiq.select(String resource, 
                   List<String> props, 
                   Object where, 
                   SortSpec sort, 
                   ResponseHandler handler)
```

### Parameters

Name | Type | Required | Description
:--: | :--: | :------:| -----------
resource | String | Yes | The resource to query
props| List<String> | No | Specifies the desired properties to be returned in each record.  An empty list or null value means all properties will be returned.
where | Object | No | Specifies constraints to filter the data.  Null means all records will be returned.  This object uses [Gson](https://github.com/google/gson) to convert to a JSON value for the REST API.
sort | SortSpec | No | Specifies the desired sort for the result set
handler | ResponseHandler | Yes | Listener that is called upon success or failure

The `props` is an list of property names indciating which properties should be returned.
    
The `where` is an object with supported operations defined in [API operations](https://dev.vantiq.com/docs/api/developer.html#api-operations) that will be converted to JSON using [Gson](https://github.com/google/gson).

The `sort` is a `SortSpec` object that indicate the property to sort by and the direction of the sort (i.e. ascending or descending).

### Returns

The `select` method calls `handler.onSuccess` with a body of type `List<JsonObject>` providing the list of matching 
records.  Upon a server failure, the `handler.onError` is called.  Upon any client exception, the `handler.onFailure` is called.

### Example

Select the `name` property for all available types.

```java
vantiq.select(Vantiq.SystemResources.TYPES.value(), 
              Collections.singletonList("name"), 
              null, 
              null, 
              new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        for(JsonObject obj : (List<JsonObject>) body) {
            System.out.println(obj.get("name").getAsString());
        }
    }
    
});
```

Selects all properties filtering to only return types with the `TestType` name.

```java
JsonObject where = new JsonObject();
where.addProperty("name", "TestType");

vantiq.select(Vantiq.SystemResources.TYPES.value(), 
              null, 
              where, 
              null, 
              new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        for(JsonObject obj : (List<JsonObject>) body) {
            System.out.println(obj);
        }
    }
    
});
```

Selects all records for the `TestType` type returning only the `key` and `value` properties and sorting the results by the `key` in descending order.

```java
vantiq.select(Vantiq.SystemResources.TYPES.value(), 
              Arrays.asList("key", "value"), 
              null, 
              new SortSpec("key", true), 
              new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        for(JsonObject obj : (List<JsonObject>) body) {
            System.out.println(obj);
        }
    }
    
});
```

## <a id="vantiq-selectOne"></a> Vantiq.selectOne

The `selectOne` method issues a query to return the single record identified 
by the given identifier.

### Signature

```java
void vantiq.selectOne(String resouce, String id, ResponseHandler handler)
```

### Parameters

Name | Type | Required | Description
:--: | :--: | :------:| -----------
resource | String | Yes | The resource to query
id   | String | Yes | The id for the given record
handler | ResponseHandler | Yes | Listener that is called upon success or failure

### Returns

The `selectOne` method calls `handler.onSuccess` with a body of type `JsonObject` providing the
matching record.  If no matching record is found, then `handler.onError` is returned with an HTTP
status code of *404*.

Upon a server failure, the `handler.onError` is called.  Upon any client exception, the `handler.onFailure` is called.

### Example

Select the `TestType` definition from the `types` resource.

```java
vantiq.selectOne(Vantiq.SystemResources.TYPES.value(), "TestType", new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println((JsonObject) obj);
    }
    
});
```

Selects a `TestType` record with `_id` equal to `23425231ad31f`.

```java
vantiq.selectOne("TestType", "23425231ad31f", new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println((JsonObject) obj);
    }
    
});
```

## <a id="vantiq-count"></a> Vantiq.count

The `count` method is similar to the `select` method except it returns only the
number of records rather than returning the records themselves.

### Signature

```java
void vantiq.count(String resource, Object where, ResponseHandler handler)
```

### Parameters

Name | Type | Required | Description
:--: | :--: | :------:| -----------
resource | String | Yes | The resource to query
where | Object | No | Specifies constraints to filter the data.  Null means all records will be returned.
handler | ResponseHandler | Yes | Listener that is called upon success or failure

The `where` is an object with supported operations defined in [API operations](https://dev.vantiq.com/docs/api/developer.html#api-operations) that will be converted to JSON using [Gson](https://github.com/google/gson).

### Returns

The `count` method calls `handler.onSuccess` with a body of type `Integer` providing the
count of the matching records.

Upon a server failure, the `handler.onError` is called.  Upon any client exception, the `handler.onFailure` is called.

### Example

Counts the number of `TestType` records.

```java
vantiq.count("TestType", null, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("TestType has " + (Integer) body + " records");
    }
    
});
```

Counts the number of `TestType` with a `value` greater than 10.

```java
JsonObject where = new JsonObject();
  JsonObject gt = new JsonObject();
  gt.addProperty("$gt", 10);
where.add("value", gt);

vantiq.count("TestType", where, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("TestType has " + (Integer) body + " records with value > 10");
    }
    
});
```

## <a id="vantiq-insert"></a> Vantiq.insert

The `insert` method creates a new record of a given resource.

### Signature

```java
void vantiq.insert(String resource, Object object, ResponseHandler handler)
```

### Parameters

Name | Type | Required | Description
:--: | :--: | :------:| -----------
resource | String | Yes | The resource to insert
object | Object | Yes | The object to insert.  This is converted to JSON using Gson.
handler | ResponseHandler | Yes | Listener that is called upon success or failure

### Returns

The `insert` method calls `handler.onSuccess` with a `JsonObject` object containing the object just inserted.  

Upon a server failure, the `handler.onError` is called.  Upon any client exception, the `handler.onFailure` is called.

### Example

Inserts an object of type `TestType`.

```
JsonObject objToInsert = new JsonObject();
objToInsert.addProperty("key", "SomeKey");
objToInsert.addProperty("value", 42);
    
vantiq.insert("TestType", objToInsert, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println((JsonObject) obj);
    }

});
```

## <a id="vantiq-update"></a> Vantiq.update

The `update` method updates an existing record of a given resource.  This method
supports partial updates meaning that only the properties provided are updated.
Any properties not specified are not changed in the underlying record.

### Signature

```java
void vantiq.update(String resource, String id, Object object, ResponseHandler handler)
```

### Parameters

Name | Type | Required | Description
:--: | :--: | :------:| -----------
resource | String | Yes | The resource to update
id   | String | Yes | The "_id" internal identifier for the record
props | Object | Yes | The properties to update in the record.  This is converted to JSON using Gson.
handler | ResponseHandler | Yes | Listener that is called upon success or failure

### Returns

The `update` method calls `handler.onSuccess` with a `JsonObject` object containing the object just updated.  

Upon a server failure, the `handler.onError` is called.  Upon any client exception, the `handler.onFailure` is called.

### Example

Updates a given `TestType` record

```java
String _id = "56f4c52120eb8b5dee4898fd";

JsonObject propsToUpdate = new JsonObject();
propsToUpdate.addProperty("value", 13);
    
vantiq.update("TestType", _id, propsToUpdate, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println((JsonObject) obj);
    }

});
```

## <a id="vantiq-upsert"></a> Vantiq.upsert

The `upsert` method either creates or updates a record in the database depending
if the record already exists.  The method tests for existence by looking at the
natural keys defined on the resource.

### Signature

```java
void vantiq.upsert(String resource, Object object, ResponseHandler response)
```

### Parameters

Name | Type | Required | Description
:--: | :--: | :------:| -----------
resource | String | Yes | The resource to upsert
object | Object | Yes | The object to upsert.  This is converted to JSON using Gson.
handler | ResponseHandler | Yes | Listener that is called upon success or failure

### Returns

The `upsert` method calls `handler.onSuccess` with a `JsonObject` object containing the object just inserted or created.  Upon a server failure, the `handler.onError` is called.  Upon any client exception, the `handler.onFailure` is called.

### Example

Inserts an object of type `TestType`.

```java
JsonObject objToUpsert = new JsonObject();
objToUpsert.addProperty("key", "SomeKey");
objToUpsert.addProperty("value", 42);
    
vantiq.upsert("TestType", objToUpsert, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println((JsonObject) obj);
    }

});
```

## <a id="vantiq-delete"></a> Vantiq.delete

The `delete` method removes records from the system for a given resource.  Deletes always
require a constraint indicating which records to remove.

### Signature

```java
void vantiq.delete(String resource, Object where, ResponseHandler handler)
```

### Parameters

Name | Type | Required | Description
:--: | :--: | :------:| -----------
resource | String | Yes | The resource to remove
where | Object | Yes | Specifies which records to remove.  This is converted to JSON using Gson.
handler | ResponseHandler | Yes | Listener that is called upon success or failure

The `where` is an object with supported operations defined in [API operations](https://dev.vantiq.com/docs/api/developer.html#api-operations) that will be converted to JSON using [Gson](https://github.com/google/gson).

### Returns

The `delete` method calls `handler.onSuccess` when the removal succeeded.  Upon a server failure, the `handler.onError` is called.  Upon any client exception, the `handler.onFailure` is called.

### Example

Removes the record with the given key.

```java
JsonObject where = new JsonObject();
where.addProperty("key", "SomeKey");

vantiq.delete("TestType", where, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("Delete succeeded.");
    }

});
```

Removes all records of `TestType` with a `value` greater than 10.

```java
JsonObject where = new JsonObject();
  JsonObject gt = new JsonObject();
  gt.addProperty("$gt", 10);
where.add("value", gt);

vantiq.delete("TestType", where, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("Deleted all records with value > 10.");
    }

});
```

## <a id="vantiq-selectOne"></a> Vantiq.deleteOne

The `deleteOne` method removes a single record specified by the given identifier.

### Signature

```java
void vantiq.deleteOne(String resource, String id, ResponseHandler response)
```

### Parameters

Name | Type | Required | Description
:--: | :--: | :------:| -----------
resource | String | Yes | The resource to remove
id   | String | Yes | The id for the given record
handler | ResponseHandler | Yes | Listener that is called upon success or failure

### Returns

The `deleteOne` method calls `handler.onSuccess` when the removal succeeded.  Upon a server failure, the `handler.onError` is called.  Upon any client exception, the `handler.onFailure` is called.

### Example

Removes the `TestType` definition from the `types` resource.

```java
vantiq.deleteOne(Vantiq.SystemResources.TYPES.value(), "TestType", new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("Delete succeeded.");
    }

});
```

Removes the `TestType` record with `_id` equal to `23425231ad31f`.

```java
vantiq.deleteOne("TestType", "23425231ad31f", new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("Delete succeeded.");
    }

});
```

## <a id="vantiq-publish"></a> Vantiq.publish

The `publish` method supports publishing to a topic or a source.

Publishing a message to a given topic allows for rules to be defined
that trigger based on the publish event.  Topics are slash-delimited strings, 
such as '/test/topic'.  Vantiq reserves  `/type`, `/property`, `/system`, and 
`/source` as system topic namespaces and should not be published to.  The 
`payload` is the message to be sent.

Calling `publish` on a source performs a publish (asynchronous call) on the
specified source.  The `payload` is the parameters required to issue the
publish operation on the source.

### Signature

```java
void vantiq.publish(String resource, String id, Object payload, ResponseHandler response)
```

### Parameters

Name | Type | Required | Description
:--: | :--: | :------:| -----------
resource | String | Yes | The resource to publish.  Must be either 'topics' or 'sources'.
id       | String | Yes | The id for the specific resource to use.  An example topic is '/test/topic'.  An example source is 'mqttChannel'.
payload  | Object | Yes | For topics, the payload is the message to send.  For sources, this is the parameters for the source.  This object is converted to JSON using Gson.
handler | ResponseHandler | Yes | Listener that is called upon success or failure

For sources, the parameters required are source specific and are the same as those required
when performing a `PUBLISH ... TO SOURCE ... USING params`.  Please refer to the specific source definition
documentation in the [Vantiq API Documentation](https://dev.vantiq.com/docs/api/index.html).

### Returns

Since the `publish` operation is an asynchronous action, a successful publish will result in the 
`handler.onSuccess` call with a boolean value.  Upon a server failure, the `handler.onError` is called.  Upon any client exception, the `handler.onFailure` is called.

### Example

Send a message onto the `/test/topic` topic.

```java
JsonObject message = new JsonObject();
message.addProperty("key", "AnotherKey");
message.addProperty("value", 13);

vantiq.publish(Vantiq.SystemResources.TOPICS.value(), 
               "/test/topic", 
               message,
               new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("Published message successfully");
    }

});
```

Send a message to a SMS source `mySMSSource`.

```java
JsonObject params = new JsonObject();
params.addProperty("body", "Nice message to the user");
params.addProperty("to", "+16505551212");

vantiq.publish(Vantiq.SystemResources.SOURCES.value(), 
               "mySMSSource", 
               params,
               new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("Published to source successfully");
    }

});
```

## <a id="vantiq-execute"></a> Vantiq.execute

The `execute` method executes a procedure on the Vantiq server.  Procedures can
take parameters (i.e. arguments) and produce a result.

### Signature

```java
void vantiq.execute(String procedure, Object params, ResponseHandler response)
```

### Parameters

Name | Type | Required | Description
:--: | :--: | :------:| -----------
procedure | String | Yes | The procedure to execute
params | Object | No | An object that holds the parameters. This object is converted to JSON using Gson.
handler | ResponseHandler | Yes | Listener that is called upon success or failure

The parameters may be provided as an array where the arguments are given in order.
Alternatively, the parameters may be provided as an object where the arguments
are named.

### Returns

The `execute` method calls `handler.onSuccess` with a `JsonObject` object containing the result of the procedure
call.  Upon a server failure, the `handler.onError` is called.  Upon any client exception, the `handler.onFailure` is called.

### Example

Executes an `sum` procedure that takes `arg1` and `arg2` arguments and returns the total using positional arguments.

```java
JsonArray params = new JsonArray();
params.add(new JsonPrimitive(1));
params.add(new JsonPrimitive(2));

vantiq.execute("sum", params, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("The sum of 1 + 2 = " + this.getBodyAsJsonObject().get("total"));
    }

});
```

Using named arguments.

```java
JsonObject params = new JsonObject();
params.addProperty("arg1", 1);
params.addProperty("arg1", 2);

vantiq.execute("sum", params, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("The sum of 1 + 2 = " + this.getBodyAsJsonObject().get("total"));
    }

});
```

## <a id="vantiq-query"></a> Vantiq.query

The `query` method performs a query (synchronous call) on the specified source.  The query can
take parameters (i.e. arguments) and produce a result.

### Signature

```java
void vantiq.query(String source, Object params, ResponseHandler handler)
```

### Parameters

Name | Type | Required | Description
:--: | :--: | :------:| -----------
source | String | Yes | The source to perform the query
params | Object | No | An object that holds the parameters. This object is converted to JSON using Gson.
handler | ResponseHandler | Yes | Listener that is called upon success or failure

The parameters required are source specific and are the same as those required
when performing a `SELECT ... FROM SOURCE ... WITH params`.  Please refer to the specific source definition
documentation in the [Vantiq API Documentation](https://dev.vantiq.com/docs/api/index.html).

### Returns

The `query` method calls `handler.onSuccess` with a `JsonObject` object containing the returned value from the source.  Upon a server failure, the `handler.onError` is called.  Upon any client exception, the `handler.onFailure` is called.

### Example

Query a REST source `adder` that returns the total of the given parameters.

```java
JsonObject params = new JsonObject();
params.addProperty("path", "/api/adder");
params.addProperty("method", "POST");
params.addProperty("contentType", "application/json");

  JsonObject body = new JsonObject();
  body.addProperty("arg1", 1);
  body.addProperty("arg2", 2);
  
params.add("body", body);

vantiq.query("sum", params, new BaseResponseHandler() {

    @Override public void onSuccess(Object body, Response response) {
        super.onSuccess(body, response);
        System.out.println("The sum of 1 + 2 = " + this.getBodyAsJsonObject().get("total"));
    }

});
```

# <a id="ResponseHandler"></a> ResponseHandler

The `ResponseHandler` interface provides the API that are called when the Vantiq SDK methods complete either
successfully or in error.

### Methods

Name | Description
---- | -----------
onSuccess | Called when the method call returns successfully (i.e. HTTP status codes 2xx).  The response body is parsed and provided in the `body` argument.
onError   | Called when the server returns one or more errors (i.e. HTTP status codes 4xx/5xx).
onFailure | Called when the client has an exception during processing.

# <a id="VantiqError"></a> VantiqError

The REST API provides an error in the following form:

    {
        code: <ErrorIdentifier>,
        message: <ErrorMessage>,
        params: [ <ErrorParameter>, ... ]
    }
    
The SDK returns errors using the `VantiqError` object.

### Parameters

Name | Type | Description
:--: | :--: | -----------
code | String | The Vantiq error id (e.g. _"io.vantiq.authentication.failed"_)
message | String | The human readable message associated with the error
params | List\<Object\> | A list of arguments associated with the error