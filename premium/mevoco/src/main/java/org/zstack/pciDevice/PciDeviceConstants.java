package org.zstack.pciDevice;

import org.zstack.header.configuration.PythonClass;

/**
 * Created by weiwang on 10/07/2017.
 */
public class PciDeviceConstants {
    @PythonClass
    public static final String SERVICE_ID = "pciDevice";
    public static final String ACTION_CATEGORY = "pciDevice";
    public static final String PCI_DEVICE_ALLOCATOR_STRATEGY = "PciDeviceStrategy";

    public static final String SYNC_PCI_DEVICE_SIGNATURE = "sync-pci-device-in-host";

    // pci quota
    public static final String GENERIC_PCI_DEVICE_NUMBER = "pci.num";
    public static final String GPU_NUMBER = "gpu.num";

    public static final int ROM_MAX_LENGTH = 16777215;

    public static final String GPU_Video_Controller = "GPU_Video_Controller";
    public static final String GPU_Audio_Controller = "GPU_Audio_Controller";
    public static final String GPU_USB_Controller = "GPU_USB_Controller";
    public static final String GPU_Serial_Controller = "GPU_Serial_Controller";
    public static final String GPU_3D_Controller = "GPU_3D_Controller";
    public static final String Ethernet_Controller = "Ethernet_Controller";
    public static final String Audio_Controller = "Audio_Controller";
    public static final String USB_Controller = "USB_Controller";
    public static final String Serial_Controller = "Serial_Controller";
    public static final String RAID_Controller = "RAID_Controller";
    public static final String SATA_Controller = "SATA_Controller";
    public static final String Memory_Controller = "Memory_Controller";
    public static final String Non_Volatile_Memory_Controller = "Non_Volatile_Memory_Controller";
    public static final String Fibre_Channel = "Fibre_Channel";
    public static final String Moxa_Device = "Moxa_Device";
    public static final String System_Peripheral = "System_Peripheral";
    public static final String ISA_Bridge = "ISA_Bridge";
    public static final String PCI_Bridge = "PCI_Bridge";
    public static final String Host_Bridge = "Host_Bridge";
    public static final String Performance_Counters = "Performance_Counters";
    public static final String Signal_Processing_Controller = "Signal_Processing_Controller";
    public static final String Communication_Controller = "Communication_Controller";
    public static final String PIC = "PIC";
    public static final String SMBus = "SMBus";
    public static final String Generic = "Generic";
    public static final String Custom = "Custom";

    public static final String CUSTOM_PCI_DEVICES = "mevoco/pciDevice/customPciDevices.xml";

    // virtual constants
    public static final String UNVIRTUALIZABLE              = "UNVIRTUALIZABLE";
    public static final String SRIOV_VIRTUALIZABLE          = "SRIOV_VIRTUALIZABLE";
    public static final String VFIO_MDEV_VIRTUALIZABLE      = "VFIO_MDEV_VIRTUALIZABLE";
    public static final String SRIOV_VIRTUALIZED            = "SRIOV_VIRTUALIZED";
    public static final String VFIO_MDEV_VIRTUALIZED        = "VFIO_MDEV_VIRTUALIZED";
    public static final String SRIOV_VIRTUAL                = "SRIOV_VIRTUAL";
    public static final String VIRTUALIZED_BYPASS_ZSTACK    = "VIRTUALIZED_BYPASS_ZSTACK";
    public static final String UNKNOWN                      = "UNKNOWN";

    public static final String PCI_VIRT_TECH_SR_IOV = "SR_IOV";
    public static final String PCI_VIRT_TECH_VFIO_MDEV = "VFIO_MDEV";

    public static final String CHECK_AND_RESERVE_PCI_DEVICE_FOR_VM = "check-and-reserve-pci-device-for-vm";

    public static final String OPERATION_ADD = "add";
    public static final String OPERATION_SUB = "sub";

}
