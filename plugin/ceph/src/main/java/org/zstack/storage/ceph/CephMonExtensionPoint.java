package org.zstack.storage.ceph;

import org.zstack.header.core.Completion;
import org.zstack.storage.ceph.backup.CephBackupStorageMonInventory;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonInventory;

/**
 * Created by mingjian.deng on 2017/11/22.
 */
public interface CephMonExtensionPoint {
    void addMoreAgentInPrimaryStorage(CephPrimaryStorageMonInventory inventory, Completion completion);
    void addMoreAgentInBackupStorage(CephBackupStorageMonInventory inventory, Completion completion);
}
