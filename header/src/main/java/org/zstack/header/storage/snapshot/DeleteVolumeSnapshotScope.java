package org.zstack.header.storage.snapshot;

public enum DeleteVolumeSnapshotScope {
    Single("single"),
    Chain("chain"),
    Auto("auto");

    private final String scope;

    DeleteVolumeSnapshotScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        return scope;
    }
}
