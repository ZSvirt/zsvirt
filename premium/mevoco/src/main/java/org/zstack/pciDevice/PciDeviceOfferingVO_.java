package org.zstack.pciDevice;

/**
 * Created by weiwang on 07/07/2017.
 */

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PciDeviceOfferingVO.class)
public class PciDeviceOfferingVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PciDeviceOfferingVO, String> name;
    public static volatile SingularAttribute<PciDeviceOfferingVO, String> description;
    public static volatile SingularAttribute<PciDeviceOfferingVO, PciDeviceOfferingType> type;
    public static volatile SingularAttribute<PciDeviceOfferingVO, String> vendorId;
    public static volatile SingularAttribute<PciDeviceOfferingVO, String> deviceId;
    public static volatile SingularAttribute<PciDeviceOfferingVO, String> subvendorId;
    public static volatile SingularAttribute<PciDeviceOfferingVO, String> subdeviceId;
    public static volatile SingularAttribute<PciDeviceOfferingVO, String> ramSize;
    public static volatile SingularAttribute<PciDeviceOfferingVO, Timestamp> createDate;
    public static volatile SingularAttribute<PciDeviceOfferingVO, Timestamp> lastOpDate;
}
