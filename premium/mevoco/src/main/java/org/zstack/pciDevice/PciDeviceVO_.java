package org.zstack.pciDevice;

import org.zstack.header.vo.ResourceVO_;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by weiwang on 07/07/2017.
 */
@StaticMetamodel(PciDeviceVO.class)
public class PciDeviceVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PciDeviceVO, String> name;
    public static volatile SingularAttribute<PciDeviceVO, String> description;
    public static volatile SingularAttribute<PciDeviceVO, String> hostUuid;
    public static volatile SingularAttribute<PciDeviceVO, String> parentUuid;
    public static volatile SingularAttribute<PciDeviceVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<PciDeviceVO, String> pciSpecUuid;
    public static volatile SingularAttribute<PciDeviceVO, PciDeviceType> type;
    public static volatile SingularAttribute<PciDeviceVO, PciDeviceState> state;
    public static volatile SingularAttribute<PciDeviceVO, PciDeviceStatus> status;
    public static volatile SingularAttribute<PciDeviceVO, PciDeviceVirtStatus> virtStatus;
    public static volatile SingularAttribute<PciDeviceVO, PciDevicePassThroughState> passThroughState;
    public static volatile SingularAttribute<PciDeviceVO, PciDeviceChooser> chooser;
    public static volatile SingularAttribute<PciDeviceVO, String> vendorId;
    public static volatile SingularAttribute<PciDeviceVO, String> vendor;
    public static volatile SingularAttribute<PciDeviceVO, String> deviceId;
    public static volatile SingularAttribute<PciDeviceVO, String> device;
    public static volatile SingularAttribute<PciDeviceVO, String> subvendorId;
    public static volatile SingularAttribute<PciDeviceVO, String> subdeviceId;
    public static volatile SingularAttribute<PciDeviceVO, String> pciDeviceAddress;
    public static volatile SingularAttribute<PciDeviceVO, String> iommuGroup;
    public static volatile SingularAttribute<PciDeviceVO, String> metaData;
    public static volatile SingularAttribute<PciDeviceVO, Timestamp> createDate;
    public static volatile SingularAttribute<PciDeviceVO, Timestamp> lastOpDate;
}
