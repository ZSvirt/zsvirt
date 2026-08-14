package org.zstack.storage.device.iscsi;

import org.zstack.header.storageDevice.ScsiLunVO_;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Create by weiwang at 2018/8/2
 */
@StaticMetamodel(IscsiLunVO.class)
public class IscsiLunVO_ extends ScsiLunVO_ {
    public static volatile SingularAttribute<IscsiLunVO, String> iscsiTargetUuid;
}
