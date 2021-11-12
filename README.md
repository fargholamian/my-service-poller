## Simple Service Poller using Vert.x 4

This is my first project written by Vert.x 4 toolkit.
This project is designed to learn and show off my learning skill.

This project is written by Vert.x 4, HTML, CSS, Pure Javascript and uses MySQL for data storage. It's a simple service poller that keeps a list of services (deﬁned by the Name and URL), and periodically performs HTTP GET requests to each and stores a record of the response ("OK" or "FAIL"). Apart from the polling logic, currently, it has a simple web page for visualizing and managing all the services together with their status.

## How it works :

* After startup, user can connect to poller via the browser (default: http://127.0.0.1:7000)
* User can change the default port with change`http.port` in `application.properties`
* User can add/edit/delete services via GUI.
* All services are saved in `T_SERIVCES` table in database.
* Each service has the following properties (saved in database):
  * `Id` : is a unique identifier (AUTO_INCREMENT)
  * `Name` : arbitrary name for service (NOT NULL)
  * `URL` : URL of the service
  * `Status` : status of the service (`OK` if accessible, `FAIL` if not reachable, `UNKNOWN` if not polled yet)
  * `Created Time` : Time of creation of the service
  * `Modified Time` : Time of last modification of the service
  * `Updated Time` : Time of last Status change
* Each service should be defined by URL and Name.
* Name and URL validation is done in frontend.
* UI connect to backend via REST APIs. APIs are included:
  * `GET /services` : Get all information of services from database
  * `POST /service` : Add new service
  * `PATCH /service`: Modify an existing service
  * `DELETE /service`: Delete an existing service
* Backend application poll all services periodically (`poller.endpoint.polling.interval`).
* Backend application save any changes in status of services in `T_STATUS_HISTORY`.
* UI poll backend services every 5 second to update services' status.


## Service API :

* `GET /services` : Get all information of services from database
* `POST /service` : Add new service
* `PATCH /service` : Modify an existing service
* `DELETE /service` : Delete an existing service

## Architecture :

Everything begins with MainVerticle (`MainVertical.java`).
In this verticle, all the verticles deploy sequentially (`compose`).

* First: `ConfigurationManager` retreive configuration parameter from `application.properties`
* Second: `FlywayInitializer` check for database migration. Migration is done by [FlywayDB](https://flywaydb.org). Migration scripts should be placed in `resources/db/migration` with specific naming patterns. Database configuration parameters are in `application.properties`.
* Third: `ServiceDaoVerticle` consume on event bus and manage CRUD APIs <--> database communication. It extends an abstract class `DaoAbstrctVerticle`. the abstract class contain common objects and function for connecting to MySQL DB. Database configuration parameters are in `application.properties`
* Forth: `ServiceChangeHistoryDaoVerticle` consume on event bus and manage Poller <--> database communication. Currently, it only receives change service status event from poller and insert them in `T_StATUS_HISTORY`. It also extends an abstract class `DaoAbstrctVerticle`. Database configuration parameters are in `application.properties`
* Fifth: `PollerVerticle` periodically read list of services from database, poll them and update changes.
  * To fewer database polling, Polling the database and services are asynchronous.
  * By default, list of services is updated every 1 minute ( will change by `poller.db.polling.interval` in `application.properties`)
  * By default, status of the services is updated every 5 seconds ( will change by `poller.endpoint.polling.interval` in `application.properties`)
  * These 2 routines share the services list via Vertx ShareData and LocalMap.
  * Being asynchronous means that new created services are polled at most 1 min later and not immediately (`# TODO: Send a message on eventbus for Poller and update the list immediatly`)
  * Both routines execute right after Verticle startup and after that scheduled by Vertx `setPeriodic`.
  * Service Poller sets a timeout for WebClient to not block for long time. It's by default 5 second (will change by `poller.polling.timeout` in `application.properties`)
  * Service Poller creates a pool of 5 WebClients by default (will change by `poller.web.client.max.poll.size` in `application.properties`)
* Sixth: `ServerVerticle` creates a HTTP server and handle routing for receiving requests:
  * Server starts by default on port 7000 (will change by `http.port` in `application.properties`)
  * Default route is handled by Vertx `StaticHandler`. It returns `html.index` in `webroot` directory.
  * Vertx `BodyHandler` is activated, so requests body is accessibly by `RoutingContext`
  * Handlers of any request types catch the requests, send the messages to `ServiceDAOVerticle` (Database manager) via event bus, receive the responses and send them back to caller.

###Other useful information:
* FrontEnd includes:
  * A single HTML page (`index.html` in webroot): It is created from a template and uses Bootstrap modal.
  * A small Pure JavaScript (`index.js`in webroot/static). It calls backend APIs and fills UI elements.
  * A small CSS style for animating notifications.
* Main page is refreshed every 5 second and gets the last status of the services from the backend (No need for reloading the page manually)
* After Adding/Modifying/Deleting the services, a Notification shows to the user (Only for the user that changes the service)
* Service information are transferring in JSON objects format.
* Logging is done by `java.util.logging`. configuration file resides in resources directory (`vertx-default-jul-logging.properties`).

## Done Stories:

The Following stories have been done in the current version:
* A user needs to be able to add a new service with a URL and a name
* Added services have to be kept when the server is restarted
* Display the name, url, creation time and status for each service
* Users want full create/update/delete functionality for services
* The results from the poller are automatically shown to the user (`#TODO : Connect UI to Back with WebSocket`)
* Users want to have informative and nice looking animations on add/remove services
* The service properly handles concurrent writes
  * Each status update record has a timestamp (corresponding to it's polling time).
  * Status update on database occurs if the timestamp of the received record is greater than the update time of record in the database.
  * Each change in status of services save independently in the History Table.
* Protect the poller from misbehaving services
  * A timeout is set for WebClients (Default: 5 sec - Configurable also)
* URL Validation
  * It's done in front-end (Javascript)

## TODO Lists
* Writing Unit, Integration and End2End Tests
* URL Validation in backend for each APIs
* Multi user support (`JWT`)
* Better UI using FrontEnd framework likes ReactJs or AngularJs.
* WebSocket for updating periodic changes (reducing API calls)
* Don't poll database for updating the list of services. Send a notification for poller vertical and udpate list of services eventually.
* Using DTO pattern
* Using ORM (Reactive Hibernate)
* Notifying service modification to all users


 ## How To Run
For now, the project is here for you to read its code, not **at all** for production use.

* Before running the project, There should be existed an instance of MySQL. if there is not any, it can be deployed by `docker-compose` script located in the root directory of the project:
  ```
  docker-compose -f mysql-compose.yml up -d
  ```
  it will deploy an instance of last version of mysql in a docker container. It exposes `3306` port for connecting to the database. Also, database files will mount outside the docker container in `./db_files`.


* Migration of database schema will be done automatically by the app after running.


* To run the app on local machinet, type in
  ```
  gradle wrapper
  ./gradlew clean run
  ```
  from the command-line. or
  ```
  gradle clean run
  ```

## Contributing

The project is completely free. You can fork the project, use it for your own purpose if you want to.

