package org.zstack.pciDevice.specification.mdev;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 2019-04-18.
 */
@StaticMetamodel(PciDeviceMdevSpecRefVO.class)
public class PciDeviceMdevSpecRefVO_ {
    public static volatile SingularAttribute<PciDeviceMdevSpecRefVO, Long> id;
    public static volatile SingularAttribute<PciDeviceMdevSpecRefVO, String> pciDeviceUuid;
    public static volatile SingularAttribute<PciDeviceMdevSpecRefVO, String> mdevSpecUuid;
    public static volatile SingularAttribute<PciDeviceMdevSpecRefVO, Boolean> effective;
    public static volatile SingularAttribute<PciDeviceMdevSpecRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<PciDeviceMdevSpecRefVO, Timestamp> lastOpDate;
}
