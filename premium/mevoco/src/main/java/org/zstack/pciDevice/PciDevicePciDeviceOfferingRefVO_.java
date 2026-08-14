package org.zstack.pciDevice;

/**
 * Created by weiwang on 07/07/2017.
 */

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(PciDevicePciDeviceOfferingRefVO.class)
public class PciDevicePciDeviceOfferingRefVO_ {
    public static volatile SingularAttribute<PciDevicePciDeviceOfferingRefVO, Long> id;
    public static volatile SingularAttribute<PciDevicePciDeviceOfferingRefVO, String> pciDeviceUuid;
    public static volatile SingularAttribute<PciDevicePciDeviceOfferingRefVO, String> pciDeviceOfferingUuid;
}
