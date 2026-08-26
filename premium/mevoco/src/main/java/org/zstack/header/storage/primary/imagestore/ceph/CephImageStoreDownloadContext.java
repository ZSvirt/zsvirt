package org.zstack.header.storage.primary.imagestore.ceph;

import org.zstack.header.storage.primary.MediatorDownloadParam;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageInventory;
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO;

public class CephImageStoreDownloadContext {
    private MediatorDownloadParam param;
    private ImageStoreBackupStorageInventory backupStorage;
    private CephPrimaryStorageVO primaryStorage;
    private String backupStorageHostname;
    private int concurrency;

    public MediatorDownloadParam getParam() {
        return param;
    }

    public void setParam(MediatorDownloadParam param) {
        this.param = param;
    }

    public ImageStoreBackupStorageInventory getBackupStorage() {
        return backupStorage;
    }

    public void setBackupStorage(ImageStoreBackupStorageInventory backupStorage) {
        this.backupStorage = backupStorage;
    }

    public CephPrimaryStorageVO getPrimaryStorage() {
        return primaryStorage;
    }

    public void setPrimaryStorage(CephPrimaryStorageVO primaryStorage) {
        this.primaryStorage = primaryStorage;
    }

    public String getBackupStorageHostname() {
        return backupStorageHostname;
    }

    public void setBackupStorageHostname(String backupStorageHostname) {
        this.backupStorageHostname = backupStorageHostname;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }
}
