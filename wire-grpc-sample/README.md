gRPC Sample Project: the Whiteboard
=========

This sample project demonstrates how [Wire](https://github.com/square/wire/) provides support for gRPC bidirectional connections, backed by proto buffers. The project consists of 3 modules:

Protos
------------

The `protos` module is responsible for 1) storing the `.proto`. files and 2) generating the data objects, shared among both client and server modules.

Misk Grpc Service
------------

The `server` module is a server powered by [Misk](https://github.com/cashapp/misk) which plays the gRPC backend of our sample project.
To start the service, use java 11 and follow these steps:

  1. Run `./gradlew server:run`.
  2. Open `https://localhost:8443`.

Android App Client
------------

The `client` module is the client side of the project. Install the app on an emulator after having launched the server and use it! Multiple clients can connect at the same time to the server.
