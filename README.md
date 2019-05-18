# Wsite

This is the online source repository of Wsite, a lightweight and fully-customizable web server and service with a
Java-powered backend.

## Important disclaimer

In its current state, Wsite is only meant for testing and development-purposes only. Not recommended to
use in production. That being said, a demo of it is being ran at http://whizvox.me

## How to run

### Requirements

The following **must** be installed:
* Java 8 or greater: [Oracle](https://www.oracle.com/technetwork/java/javase/downloads/index.html) /
[OpenJDK](https://jdk.java.net/)

If you are having any problems building or running, use Java 8, not Java 9+.

Optional:
* [Gradle 4.x](https://gradle.org/)
* [sqlite3](https://sqlite.org/index.html)

### Creating an executable Jar

Run `./gradlew shadowJar`. The result will be in `./build/libs/wsite-<version>.jar`

### Run with Gradle

Run `./gradlew run`. The service will use `./rundir` as the working directory.

## Quick-start guide

* First running will result in a small setup service being hosted at `localhost:4568/`
* Fill in (at minimum) the fields under `Initial operator` (the SSL and SMTP fields don't do anything right now)
* Submit the form, and the server will restart with the standard routes
* Login at `/login` (logout at `/logout`)
* Every route prefixed with `/control` will require an operator to be logged in.
* Goto `/control` to view all available control options
