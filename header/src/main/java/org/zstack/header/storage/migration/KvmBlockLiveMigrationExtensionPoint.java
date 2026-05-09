package org.zstack.header.storage.migration;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeVO;

import java.util.List;
import java.util.Map;

public interface KvmBlockLiveMigrationExtensionPoint {
    ErrorCode beforeBlockLiveMigration(HostInventory dstHost,
                                       VmInstanceInventory vm,
                                       List<VolumeVO> volumesToMigrate,
                                       Map<String, String> volumeMappingDict,
                                       Map<String, String> targetVolumeLuksSecrets);
}
