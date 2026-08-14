package org.zstack.header.baremetal.chassis;

/**
 * Created by GuoYi on 2017/3/28.
 */
public interface BaremetalChassisConstant {
    String SERVICE_ID = "baremetal.chassis";
    String ACTION_CATEGORY = "baremetal.chassis";

    String SERVER_POWER_ON = "Chassis Power is on";
    String SERVER_POWER_OFF = "Chassis Power is off";
    String SERVER_POWER_UNKNOWN = "UNKNOWN";

    String SERVER_BOOT_DEV_PXE = "pxe";
    String SERVER_BOOT_DEV_DISK = "disk";

    String SEND_HARDWARE_INFO = "/baremetal/chassis/sendhardwareinfo";
    String BAREMETAL_HARDWARE_INFO_BASIC_TYPE = "basic";
    String BAREMETAL_HARDWARE_INFO_NIC_TYPE = "nic";
    String BAREMETAL_HARDWARE_INFO_DISK_TYPE = "disk";
    String BAREMETAL_HARDWARE_INFO_PXESERVER_TYPE = "pxeserver";

    String SYNC_SIGNATURE_OF_BAREMETAL_CHASSIS = "sync-signature-of-baremetal-chassis-";
}
