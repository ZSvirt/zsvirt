package org.zstack.header.network.l2;

import java.util.List;

public interface L2NetworkOwnedL3ExtensionPoint {
    /**
     * Retrieves the type of the L2 network to determine which implementation interface
     * should be used for network operations.
     *
     * @return the type of the L2 network, which is an instance of {@link L2NetworkType}.
     */
    L2NetworkType getType();
    /**
     * Retrieves all L3 network UUIDs that are exclusively owned by the specified L2 network.
     *
     * @param l2NetworkUuid the UUID of the L2 network whose L3 networks should be retrieved
     * @return a non-null list of L3 network UUIDs owned by the specified L2 network.
     *         Returns an empty list if no L3 networks exist for this L2 network.
     */
    List<String> getOwnedL3NetworkUuids(String l2NetworkUuid);
}
