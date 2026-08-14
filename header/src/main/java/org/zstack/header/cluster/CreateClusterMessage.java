package org.zstack.header.cluster;

public interface CreateClusterMessage {
    String getZoneUuid();

    String getClusterName();

    String getDescription();

    String getHypervisorType();

    String getType();

    String getResourceUuid();

    String getArchitecture();
}
