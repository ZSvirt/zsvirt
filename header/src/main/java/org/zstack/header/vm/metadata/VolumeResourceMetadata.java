package org.zstack.header.vm.metadata;

public class VolumeResourceMetadata extends ResourceMetadata {
    private String snapshotReference;
    private String snapshotReferenceTree;

    public String getSnapshotReference() {
        return snapshotReference;
    }

    public void setSnapshotReference(String snapshotReference) {
        this.snapshotReference = snapshotReference;
    }

    public String getSnapshotReferenceTree() {
        return snapshotReferenceTree;
    }

    public void setSnapshotReferenceTree(String snapshotReferenceTree) {
        this.snapshotReferenceTree = snapshotReferenceTree;
    }
}
