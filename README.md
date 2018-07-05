
To start the Akka HTTP server with Akka Clustering

1. run `./gradlew shadowJar` to create an executable jar with dependencies
2. Open a termainal to serve the Ui node of the cluster:
    - `java -jar build/libs/clients-servers-all.jar serve -s akka-http -n ClusterMember -p 8088 -c 2551`
    - Connect to the Ui by browsing to http://localhost:8088/, you should see entities appearing on node 2551
3. Open as many more terminals as you want, increment the port for each one:
    - `java -jar build/libs/clients-servers-all.jar serve -s akka-node -n ClusterMember -c 2552`
    - You should see the nodes appear on the UI, and entities allocate to it, not always balanced
4. Terminate and re-start nodes created in step 3 to see re-allocation of entities in the Akka Cluster!

Note: You'll see console errors in the UI, as cluster event's aren't handled nicely in this version, focus is on 
seeing entity distribution via Akka Cluster Sharding.

Roadmap:
1. Smarter cluster seed node initialization, currently hard coded in `application.conf`
2. UI improvements, suuper basic right now
3. Handle cluster membership info nicely in UI
