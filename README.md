# Wsite

This is the online source repository of Wsite, a lightweight, fully-customizable, Java-based web server and service.


## Quick-use guide

* Create an executable Jar by running `./gradlew shadowJar`. The result is located at `./build/libs`
* Run the Jar by issuing the following command: `java -jar wsite-<version>.jar`
* First running the Jar will result in a small setup service being hosted at `localhost:4568/`
* Fill in (at minimum) the fields under `Initial user` (the SSL and SMTP fields don't do anything right now)
* Submit the form, and the server will restart with the standard routes
* Login at `/login` (logout at `/logout`)
* Every route prefixed with `/control` will require an operator to be logged in.
* Create / delete pages at `/control/newPage` / `/control/deletePage`
* Create / delete users at `/control/newUser` / `control/deleteUser` (you can't delete yourself)
* Shutdown / restart the server at `/control/shutdown` / `/control/restart`

That's all for now. Come back later for more updates...