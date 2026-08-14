package org.zstack.header.storage.primary;

/**
 * Created by frank on 10/21/2015.
 */
public enum PrimaryStorageAllocationPurpose {
    CreateNewVm,   // Be careful: purpose CreateNewVm has very special logic
    CreateRootVolume,
    CreateDataVolume,
    DownloadSnapshot,
    DownloadImage
}
