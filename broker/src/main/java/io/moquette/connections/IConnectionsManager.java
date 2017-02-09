
package io.moquette.connections;

import java.util.Collection;

/**
 * This interface will be used by an external codebase to retrieve and close physical connections.
 *
 * @author lbarrios
 *
 */
public interface IConnectionsManager {

    /**
     * Returns the number of physical connections
     *
     * @return
     */
    public int getActiveConnectionsNo();

    /**
     * Determines wether a MQTT client is connected to the broker.
     *
     * @param clientID
     * @return
     */
    public boolean isConnected(String clientID);

    /**
     * Returns the identifiers of the MQTT clients that are connected to the broker.
     *
     * @return
     */
    public Collection<String> getConnectedClientIds();

    /**
     * Closes a physical connection.
     *
     * @param clientID
     * @param closeImmediately
     *            If false, the connection will be flushed before it is closed.
     * @return
     */
    public boolean closeConnection(String clientID, boolean closeImmediately);

    /**
     * Returns the state of the session of a given client.
     *
     * @param clientID
     * @return
     */
    public MqttSession getSessionStatus(String clientID);

    /**
     * Returns the state of all the sessions
     *
     * @return
     */
    public Collection<MqttSession> getSessions();
}
