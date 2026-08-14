package org.zstack.header.storage.snapshot;

public enum DeleteVolumeSnapshotDirection {
    Pull("pull"),
    Commit("commit"),
    Auto("auto");

    private final String direction;

    DeleteVolumeSnapshotDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        return direction;
    }

    public static DeleteVolumeSnapshotDirection fromString(String value) {
        for (DeleteVolumeSnapshotDirection direction : DeleteVolumeSnapshotDirection.values()) {
            if (direction.direction.equalsIgnoreCase(value)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("Invalid direction: " + value);
    }
}
