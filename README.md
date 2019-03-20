# Wsite

This is the online source repository of Wsite, a lightweight, fully-customizable, Java-based web server and service.

## How to run

### Creating an executable Jar

Run `./gradlew shadowJar`. The result will be in `./build/libs/wsite-<version>.jar`

### Via Gradle

Run `./gradlew run`. The service will use `./rundir` as the working directory.

## Quick-start guide

* First running will result in a small setup service being hosted at `localhost:4568/`
* Fill in (at minimum) the fields under `Initial operator` (the SSL and SMTP fields don't do anything right now)
* Submit the form, and the server will restart with the standard routes
* Login at `/login` (logout at `/logout`)
* Every route prefixed with `/control` will require an operator to be logged in.
* Upload / delete assets at `/control/uploadAsset` / `/control/deleteAsset`, publicly accessible at `/assets/<path>`
* Create / delete pages at `/control/createPage` / `/control/deletePage`
* Create / delete users at `/control/createUser` / `control/deleteUser` (you can't delete yourself)
* Configure settings at `/control/configSite` / `/control/configDatabase` / `/control/configSsl` (not used) / 
`/control/configSmtp` (not used)
* Edit stuff at `/control/edit<Asset|Page|User>`
* Edit your own user information at `/profile/edit`
* Shutdown / restart the server at `/control/shutdown` / `/control/restart`

That's all for now. Come back later for more updates...