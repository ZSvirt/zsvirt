package org.zstack.header.storage.primary;

public interface PrimaryToExternalStorageMediator {
    String getSupportedPrimaryStorageType();

    String getSupportedExternalStorageType();
}
