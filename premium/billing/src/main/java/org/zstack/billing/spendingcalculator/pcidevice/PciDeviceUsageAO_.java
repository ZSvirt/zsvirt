package org.zstack.billing.spendingcalculator.pcidevice;

import org.zstack.billing.UsageAO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/4/3.
 */

@StaticMetamodel(PciDeviceUsageAO.class)
public class PciDeviceUsageAO_ extends UsageAO_ {
    public static volatile SingularAttribute<PciDeviceUsageAO, Long> id;
    public static volatile SingularAttribute<PciDeviceUsageAO, String> pciDeviceUuid;
    public static volatile SingularAttribute<PciDeviceUsageAO, String> vendorId;
    public static volatile SingularAttribute<PciDeviceUsageAO, String> deviceId;
    public static volatile SingularAttribute<PciDeviceUsageAO, String> subvendorId;
    public static volatile SingularAttribute<PciDeviceUsageAO, String> subdeviceId;
    public static volatile SingularAttribute<PciDeviceUsageAO, String> description;
    public static volatile SingularAttribute<PciDeviceUsageAO, String> vmName;
    public static volatile SingularAttribute<PciDeviceUsageAO, String> status;
    public static volatile SingularAttribute<PciDeviceUsageAO, String> inventory;
    public static volatile SingularAttribute<PciDeviceUsageAO, Timestamp> createDate;
    public static volatile SingularAttribute<PciDeviceUsageAO, Timestamp> lastOpDate;
}
