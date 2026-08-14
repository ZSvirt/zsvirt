package org.zstack.pciDevice.specification.pci;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 2019-05-23.
 */
@StaticMetamodel(VmInstancePciDeviceSpecRefVO.class)
public class VmInstancePciDeviceSpecRefVO_ {
    public static volatile SingularAttribute<VmInstancePciDeviceSpecRefVO, Long> id;
    public static volatile SingularAttribute<VmInstancePciDeviceSpecRefVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VmInstancePciDeviceSpecRefVO, String> pciSpecUuid;
    public static volatile SingularAttribute<VmInstancePciDeviceSpecRefVO, Integer> pciDeviceNumber;
    public static volatile SingularAttribute<VmInstancePciDeviceSpecRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmInstancePciDeviceSpecRefVO, Timestamp> lastOpDate;
}
