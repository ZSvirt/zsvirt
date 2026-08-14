package org.zstack.storage.device.nvme;

import org.zstack.header.storageDevice.LunVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by MaJin on 2022/8/10.
 */

@StaticMetamodel(NvmeLunVO.class)
public class NvmeLunVO_ extends LunVO_ {
    public static volatile SingularAttribute<NvmeLunVO, String> nvmeTargetUuid;
}
