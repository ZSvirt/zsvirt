package org.zstack.pciDevice.specification.mdev;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 2019-05-23.
 */
@StaticMetamodel(VmInstanceMdevDeviceSpecRefVO.class)
public class VmInstanceMdevDeviceSpecRefVO_ {
    public static volatile SingularAttribute<VmInstanceMdevDeviceSpecRefVO, Long> id;
    public static volatile SingularAttribute<VmInstanceMdevDeviceSpecRefVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VmInstanceMdevDeviceSpecRefVO, String> mdevSpecUuid;
    public static volatile SingularAttribute<VmInstanceMdevDeviceSpecRefVO, Integer> mdevDeviceNumber;
    public static volatile SingularAttribute<VmInstanceMdevDeviceSpecRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmInstanceMdevDeviceSpecRefVO, Timestamp> lastOpDate;
}
