package org.zstack.header.image;

public interface CreateTemplateFromSnapshotMessage {
    void setSnapshotUuid(String snapshotUuid);
    String getSnapshotUuid();
}
