package org.zstack.storage.device.nvme;

import org.zstack.header.storageDevice.ScsiLunHostRefVO;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ScsiLunHostRefVO.class)
public class NvmeLunHostRefVO_ {
    public static volatile SingularAttribute<ScsiLunHostRefVO, Long> id;
    public static volatile SingularAttribute<ScsiLunHostRefVO, String> nvmeLunUuid;
    public static volatile SingularAttribute<ScsiLunHostRefVO, String> hostUuid;
}
