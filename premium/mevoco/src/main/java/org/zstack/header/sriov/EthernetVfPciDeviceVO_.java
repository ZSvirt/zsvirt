package org.zstack.header.sriov;

import org.zstack.pciDevice.PciDeviceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by shixin 2023.10.19
 */
@StaticMetamodel(EthernetVfPciDeviceVO.class)
public class EthernetVfPciDeviceVO_ extends PciDeviceVO_ {
    public static volatile SingularAttribute<EthernetVfPciDeviceVO, String> interfaceName;
    public static volatile SingularAttribute<EthernetVfPciDeviceVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<EthernetVfPciDeviceVO, String> hostDevUuid;
    public static volatile SingularAttribute<EthernetVfPciDeviceVO, String> vmUuid;
    public static volatile SingularAttribute<EthernetVfPciDeviceVO, EthernetVfStatus> vfStatus;
}
