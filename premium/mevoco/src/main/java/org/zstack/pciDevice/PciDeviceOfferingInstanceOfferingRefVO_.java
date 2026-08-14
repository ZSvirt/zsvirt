package org.zstack.pciDevice;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by weiwang on 07/07/2017.
 */
@StaticMetamodel(PciDeviceOfferingInstanceOfferingRefVO.class)
public class PciDeviceOfferingInstanceOfferingRefVO_ {
    public static volatile SingularAttribute<PciDeviceOfferingInstanceOfferingRefVO, Long> id;
    public static volatile SingularAttribute<PciDeviceOfferingInstanceOfferingRefVO, String> instanceOfferingUuid;
    public static volatile SingularAttribute<PciDeviceOfferingInstanceOfferingRefVO, String> pciDeviceOfferingUuid;
    public static volatile SingularAttribute<PciDeviceOfferingInstanceOfferingRefVO, String> metadata;
    public static volatile SingularAttribute<PciDeviceOfferingInstanceOfferingRefVO, Integer> pciDeviceCount;
}

