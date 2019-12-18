# vertx-starter-pack
Starter pack for Vert.x development. Read a [wiki](https://github.com/IASA-HUB/vertx-starter-pack/wiki) for preparation.

# Note about IDEA

[Here](https://github.com/B1Z0N/java-basics/blob/master/README.md#idea-shortcuts) you could find some IDEA shortcuts that i've found the most useful(this list is being updated continuosly).

Now let's dive in this project:

# Files

There are some important files to understand this project. Two main directories is `common` and `profiles`.

## common

Look at `build.gradle`, it is the first project configuration file. You should see project dependencies, 
their's meanings one by one:

- Service discovery - component, that provides an infrastructure to publish and discover various services
- Hazlecast - default custer manager
- Web - http server
- Web client - asynchronous http client
- Circuit breaker - implementation of [`Circuit breaker pattern`](https://en.wikipedia.org/wiki/Circuit_breaker_design_pattern)
- Health check - testing the health of the application

*Check [Useful `vert.x` concepts](#useful-vertx-concepts) section for more.

Now turn your attention to `src/main/java/vertx/common` folder. There are two files.
Start reading `Launcher.java` first and `MicroserviceVerticle.java` second.
**This whole part is just two classes intended for future reuse by inheriting from them**.
That is why it is `common` - because all this code is common for our project(and lots of other `vert.x` projects, perhaps)


## profiles

This time `profiles/build.gradle` contains pretty much the same libraries with few additions like `google` and `vert.x` libraries for `gRPC`. We won't stop here, go to `profiles/src/main/java/profiles/` folder.

`Launcher.java` is the entry point that runs `vert.x`. Other classes are placed in folders:

### model

This folder contains classes for transfering to main types of messages over `eventbus`: configuration messages and profiles messages. So here is the meaning of every class here:

- `Config` is just a data class: representation of conf/config.json in plain java class 
- `ConfigMessageCoded` is an class that implements `MessageCodec` interface - general way of transportation of custom message types over `eventbus` and this class are used concretly to transport `Config` class.
- `Profile` is just a data class: representation of user info in plain java class
- `ProfileMessageCodec` is just like an `ConfigMessageCodec` but for transporting of `Profile` class

### services

The folder contains only one file: `ProfileServiceImpl.java`. This class is a service for fetching profile. First it fetches
`Profile` object, transforms it to `ProfileObject` object, wraps it with `ProfileResponse` object and passes it to client when it is requested.

### verticles

This folder contains classes and here is theirs purpose:

- `ConfigurationVerticle` - for configuration maintenance update/retrieval
- `ServiceDiscoveryVerticle` - the name suggests the meaning, read [here](https://vertx.io/docs/vertx-service-discovery/java/) for more
- `ApiVerticle` - for settling `VertxServer`(adding service for retrieval of Profile data via gRPC)
- `ProfileVerticle` - just replying to Profile requests with {"Petr",  "Ivanov", ... }.
- `MainVerticle` - for deploying all verticles listed above

### Client

`ProfilesClient.java` is a client class that connects to our server and prints response("Petr",  "Ivanov", ... ). 

The common question is: "How do our client knows the address of our server and other configuration info?". Well, it takes all this from our `conf/config.json` file. It is not magic.

For more - read comments in code.

# Resources

This part is all about useful links to understand `vert.x` and this project of course(be patient):

## Beginner

- Unofficial [tutorial](http://tutorials.jenkov.com/vert.x/index.html)
- Official [tutorial](https://vertx.io/blog/posts/introduction-to-vertx.html)
- [Guide](https://vertx.io/docs/guide-for-java-devs/) to asynchronous programming, actually all about `vert.x`(read the first three items)

## Useful `vert.x` concepts

- [ServiceDiscovery](https://vertx.io/docs/vertx-service-discovery/java/)(read the first eight items)
- [Hazelcast](https://vertx.io/docs/vertx-hazelcast/java/)
- [Web](https://vertx.io/docs/vertx-web/java/)
- [Web client](https://vertx.io/docs/vertx-web-client/java/)
- [Circuit breaker](https://vertx.io/docs/vertx-circuit-breaker/java/)
- [Health check](https://vertx.io/docs/vertx-health-check/java/)

## Other

- What is REST - [part 1](https://medium.com/extend/what-is-rest-a-simple-explanation-for-beginners-part-1-introduction-b4a072f8740f) and [part 2](https://medium.com/extend/what-is-rest-a-simple-explanation-for-beginners-part-2-rest-constraints-129a4b69a582)
- What is [`gRPC`](https://grpc.io/docs/guides/)?
- What is [`SSL`](https://www.digicert.com/ssl/)(`TLS` is just a successor of `SSL`)?

---

**Google a lot and read a lot, other info is comments in files.**
