
To start the Akka HTTP server with Akka Clustering

1. run `./gradlew shadowJar` to create an executable jar with dependencies
2. Open two terminals, run the following two commands, one in each term.  -p sets the HTTP server port.  -c sets the Akka Cluster port
3. 'java -jar build/libs/clients-servers-all.jar serve -s akka -p 8088 -c 2551'
4. 'java -jar build/libs/clients-servers-all.jar serve -s akka -p 8089 -c 2552'

You should see clustering lifecycle events printed, if you kill one of the terms you'll see node death events.

Roadmap:
1. Smarter cluster seed node initialization, currently hard coded in `application.conf`
2. Websocket UI to visualize Akka clustering and later sharding, via Akka Http
3. Akka Sharding for exploring Modeling domain Entities
