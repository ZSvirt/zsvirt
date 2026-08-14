package org.zstack.storage.device.fibreChannel;

import org.zstack.header.storageDevice.ScsiLunVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Create by weiwang at 2018/10/18
 */
@StaticMetamodel(FiberChannelLunVO.class)
public class FiberChannelLunVO_ extends ScsiLunVO_ {
    public static volatile SingularAttribute<FiberChannelLunVO, String> fiberChannelStorageUuid;
}
