package org.zstack.header.storageDevice;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Create by weiwang at 2018/8/2
 */
@StaticMetamodel(ScsiLunVmInstanceRefVO.class)
public class ScsiLunVmInstanceRefVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ScsiLunHostRefVO, String> id;
    public static volatile SingularAttribute<ScsiLunHostRefVO, String> scsiLunUuid;
    public static volatile SingularAttribute<ScsiLunHostRefVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<ScsiLunHostRefVO, Integer> deviceId;
    public static volatile SingularAttribute<ScsiLunHostRefVO, Boolean> attachMultipath;
}
