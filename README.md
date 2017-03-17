Tipster
-------

*A simple service for submitting, retrieving, and commenting on tips*

Getting Started
===============

### Prerequisites ###

* Build and Unit Test
  * A Java 8 JDK install (tested with OpenJDK)
* Packaging
  * A Docker Engine (tested with Docker CE 17.03.0)
* Run and Integration Test
  * A Docker Engine (tested with Docker CE 17.03.0)
  * Docker Compose (tested with docker-compose 1.11.2)

### Building ###

This repository includes a copy of Paul Phillips's `sbt-extras` sbt launcher, so
you don't have to have sbt itself installed. Instead, just invoke the `sbt`
script in this directory, and an appropriate version of sbt will be downloaded
automatically and run. If you've already downloaded sbt using the script, it
will be cached.

To build the app and run all the unit tests run `./sbt test` in the project
directory.

### Packaging, Integration Testing, and Running ###

This project uses the [sbt-native-packager][sbt-native-packager] to build a
Docker image using the local Docker Engine. The Docker image can then be run
manually, which will require providing the rest of the runtime dependencies like
Postgresql to the container through environment variables or configuration, or,
more conveniently, by using the included `docker-compose.yml` file. The compose
file is also used to set up the environment to run the integration tests.

To build the docker image, run `./sbt docker:publishLocal`

To run the integration tests, run `./sbt integrate`. The integration tests will 
ensure that the app image is built and executing in the docker-compose environment.

To run the docker image using docker-compose, first build the image as above,
then run `docker-compose up` or, from the sbt console, `dockerCompose up`. The
service will be available (through the nginx proxy) at http://172.24.0.50/. The
other services in the environment will be available at automatically assigned
addresses within the 172.24.0.0/24 subnet.

To run the app in a local jvm while using the environment from the
docker-compose file, first bring up the environment: `docker-compose up`. It is
not necessary to shutdown the instance of the service running in docker, but you
can do so by running `docker-compose down tipster`. Then you can run tipster in
a local jvm by running `./sbt run`. If you want to attach a debugger, then you
can run `./sbt -jvm-debug <port> run` and then configure your IDE to connect to the
debugger on the specified port
