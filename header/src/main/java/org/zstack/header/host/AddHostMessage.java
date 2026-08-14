package org.zstack.header.host;

/**
 * Created by david on 9/12/16.
 */
public interface AddHostMessage {
    String getName();

    String getDescription();

    String getManagementIp();

    String getClusterUuid();

    String getResourceUuid();
}
