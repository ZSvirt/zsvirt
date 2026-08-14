package org.zstack.storage.volume;

public enum VolumeErrors {
    VOLUME_IN_USE(1000),
    FLATTEN_ERROR(1001);

    private String code;

    VolumeErrors(int id) {
        code = String.format("VOLUME.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
