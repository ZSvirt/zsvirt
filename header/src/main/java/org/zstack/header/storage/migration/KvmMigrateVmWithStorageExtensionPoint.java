package org.zstack.header.storage.migration;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.MigrateVmOnHypervisorMsg;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;

import java.util.List;
import java.util.Map;

public interface KvmMigrateVmWithStorageExtensionPoint {
    ErrorCode beforeMigrateVmWithStorage(HostInventory dstHost,
                                         VmInstanceInventory vm,
                                         List<VolumeVO> volumesToMigrate,
                                         Map<String, String> volumeMappingDict,
                                         MigrateVmOnHypervisorMsg msg);

    String prepareVolumeEncryptedDek(String hostUuid, VolumeInventory volume);
}
