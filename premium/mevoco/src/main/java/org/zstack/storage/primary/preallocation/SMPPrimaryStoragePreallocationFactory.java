package org.zstack.storage.primary.preallocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.primary.smp.SMPConstants;
import org.zstack.storage.primary.smp.SMPPrimaryStorageGlobalConfig;

/**
 * author:kaicai.hu
 * Date:2019/8/20
 */
public class SMPPrimaryStoragePreallocationFactory extends FileSystemPreallocationFactory {
    @Autowired
    private ResourceConfigFacade rcf;

    public static String type = SMPConstants.SMP_TYPE;

    @Override
    public String getPrimaryStorageType() {
        return type;
    }

    @Override
    public String getPreallocation(String primaryStorageUuid) {
        return rcf.getResourceConfigValue(SMPPrimaryStorageGlobalConfig.QCOW2_ALLOCATION, primaryStorageUuid, String.class);
    }

    @Override
    public String getProvisioningStrategy(String primaryStorageUuid) {
        return judgeQcow2ProvisioningStrategy(primaryStorageUuid);
    }
}
