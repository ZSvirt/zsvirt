package org.zstack.pciDevice.specification.pci;

import org.zstack.header.vo.ResourceVO_;
import org.zstack.pciDevice.PciDeviceType;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 2019-03-05.
 */
@StaticMetamodel(PciDeviceSpecVO.class)
public class PciDeviceSpecVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PciDeviceSpecVO, String> name;
    public static volatile SingularAttribute<PciDeviceSpecVO, String> description;
    public static volatile SingularAttribute<PciDeviceSpecVO, String> vendorId;
    public static volatile SingularAttribute<PciDeviceSpecVO, String> vendor;
    public static volatile SingularAttribute<PciDeviceSpecVO, String> deviceId;
    public static volatile SingularAttribute<PciDeviceSpecVO, String> device;
    public static volatile SingularAttribute<PciDeviceSpecVO, String> subvendorId;
    public static volatile SingularAttribute<PciDeviceSpecVO, String> subdeviceId;
    public static volatile SingularAttribute<PciDeviceSpecVO, String> ramSize;
    public static volatile SingularAttribute<PciDeviceSpecVO, Integer> maxPartNum;
    public static volatile SingularAttribute<PciDeviceSpecVO, PciDeviceType> type;
    public static volatile SingularAttribute<PciDeviceSpecVO, PciDeviceSpecState> state;
    public static volatile SingularAttribute<PciDeviceSpecVO, Boolean> isVirtual;
    public static volatile SingularAttribute<PciDeviceSpecVO, String> romContent;
    public static volatile SingularAttribute<PciDeviceSpecVO, String> romVersion;
    public static volatile SingularAttribute<PciDeviceSpecVO, String> romMd5sum;
    public static volatile SingularAttribute<PciDeviceSpecVO, Timestamp> createDate;
    public static volatile SingularAttribute<PciDeviceSpecVO, Timestamp> lastOpDate;
}
