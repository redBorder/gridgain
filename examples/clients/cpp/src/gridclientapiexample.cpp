/* @cpp.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

/**
 * <summary>
 * This example demonstrates use of GridGain C++ remote client API. To compile this example
 * you first need to compile the API, located in GRIDGAIN_HOME/modules/clients/cpp (see README
 * file for compilation instructions).
 * <p>
 * To execute this example you should start one or more instances of <c>ClientExampleNodeStartup</c>
 * Java class which will start up a GridGain node with proper configuration (you can compile
 * and run this class from your favourite IDE).
 * <p>
 * You can also start a stand-alone GridGain instance by passing the path
 * to configuration file to <c>ggstart.{sh|bat}</c> script, like so:
 * <c>ggstart.sh examples/config/example-cache.xml'</c>.
 * <p>
 * Note that this example requires <c>org.gridgain.examples.misc.client.api.ClientExampleTask</c>
 * class to be present in remote nodes' classpath. If remote nodes are run by <c>ggstart.{sh|bat}</c> script
 * then JAR file containing the examples code should be placed to <c>GRIDGAIN_HOME/libs</c> folder.
 * You can build <c>gridgain-examples.jar</c> by running <c>mvn package</c> in <c>GRIDGAIN_HOME/examples</c>
 * folder. After that <c>gridgain-examples.jar</c> will be generated by Maven in
 * <c>GRIDGAIN_HOME/examples/target</c> folder.
 * <p>
 * After node has been started this example creates a client connection and performs some
 * Compute Grid related operations.
 * </summary>
 */

#include "gridclientapiexample.hpp"

#include <vector>

using namespace std;

GridClientConfiguration clientConfiguration() {
    GridClientConfiguration clientConfig;

    vector<GridClientSocketAddress> servers;

    for (int i = TCP_PORT; i < TCP_PORT + MAX_NODES; i++)
        servers.push_back(GridClientSocketAddress(SERVER_ADDRESS, i));

    clientConfig.servers(servers);

    return clientConfig;
}
