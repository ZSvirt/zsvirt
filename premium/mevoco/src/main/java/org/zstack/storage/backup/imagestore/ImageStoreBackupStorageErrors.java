package org.zstack.storage.backup.imagestore;

public enum ImageStoreBackupStorageErrors {
    RECONNECT_ERROR(1500);
    private String code;

    ImageStoreBackupStorageErrors(int id) {
        code = String.format("BS.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
