package org.zstack.storage.primary.preallocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageGlobalConfig;


/**
 * author:kaicai.hu
 * Date:2019/8/20
 */
public class NfsPrimaryStoragePreallocationFactory extends FileSystemPreallocationFactory {
    @Autowired
    private ResourceConfigFacade rcf;

    public static String type = NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE;

    @Override
    public String getPrimaryStorageType() {
        return type;
    }

    @Override
    public String getPreallocation(String primaryStorageUuid) {
        return rcf.getResourceConfigValue(NfsPrimaryStorageGlobalConfig.QCOW2_ALLOCATION, primaryStorageUuid, String.class);
    }

    @Override
    public String getProvisioningStrategy(String primaryStorageUuid) {
        return judgeQcow2ProvisioningStrategy(primaryStorageUuid);
    }

}
