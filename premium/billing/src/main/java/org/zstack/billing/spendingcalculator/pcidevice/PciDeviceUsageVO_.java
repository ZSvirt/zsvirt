package org.zstack.billing.spendingcalculator.pcidevice;

/**
 * Created by shixin/ruan on 2018/05/07.
 */
import org.zstack.billing.UsageAO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PciDeviceUsageVO.class)
public class PciDeviceUsageVO_ extends UsageAO_ {
    public static volatile SingularAttribute<PciDeviceUsageVO, Long> id;
    public static volatile SingularAttribute<PciDeviceUsageVO, String> pciDeviceUuid;
    public static volatile SingularAttribute<PciDeviceUsageVO, String> vendorId;
    public static volatile SingularAttribute<PciDeviceUsageVO, String> deviceId;
    public static volatile SingularAttribute<PciDeviceUsageVO, String> subvendorId;
    public static volatile SingularAttribute<PciDeviceUsageVO, String> subdeviceId;
    public static volatile SingularAttribute<PciDeviceUsageVO, String> description;
    public static volatile SingularAttribute<PciDeviceUsageVO, String> vmUuid;
    public static volatile SingularAttribute<PciDeviceUsageVO, String> vmName;
    public static volatile SingularAttribute<PciDeviceUsageVO, String> status;
    public static volatile SingularAttribute<PciDeviceUsageVO, String> inventory;
    public static volatile SingularAttribute<PciDeviceUsageVO, Timestamp> createDate;
    public static volatile SingularAttribute<PciDeviceUsageVO, Timestamp> lastOpDate;
}
