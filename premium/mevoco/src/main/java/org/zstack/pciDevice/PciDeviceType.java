package org.zstack.pciDevice;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Created by weiwang on 07/07/2017.
 */
public enum PciDeviceType {
    GPU_Video_Controller(PciDeviceConstants.GPU_Video_Controller),
    GPU_Audio_Controller(PciDeviceConstants.GPU_Audio_Controller),
    GPU_USB_Controller(PciDeviceConstants.GPU_USB_Controller),
    GPU_Serial_Controller(PciDeviceConstants.GPU_Serial_Controller),
    GPU_3D_Controller(PciDeviceConstants.GPU_3D_Controller),
    Ethernet_Controller(PciDeviceConstants.Ethernet_Controller),
    Audio_Controller(PciDeviceConstants.Audio_Controller),
    USB_Controller(PciDeviceConstants.USB_Controller),
    Serial_Controller(PciDeviceConstants.Serial_Controller),
    RAID_Controller(PciDeviceConstants.RAID_Controller),
    SATA_Controller(PciDeviceConstants.SATA_Controller),
    Memory_Controller(PciDeviceConstants.Memory_Controller),
    Non_Volatile_Memory_Controller(PciDeviceConstants.Non_Volatile_Memory_Controller),
    Fibre_Channel(PciDeviceConstants.Fibre_Channel),
    Moxa_Device(PciDeviceConstants.Moxa_Device),
    System_Peripheral(PciDeviceConstants.System_Peripheral),
    ISA_Bridge(PciDeviceConstants.ISA_Bridge),
    PCI_Bridge(PciDeviceConstants.PCI_Bridge),
    Host_Bridge(PciDeviceConstants.Host_Bridge),
    Performance_Counters(PciDeviceConstants.Performance_Counters),
    Signal_Processing_Controller(PciDeviceConstants.Signal_Processing_Controller),
    Communication_Controller(PciDeviceConstants.Communication_Controller),
    PIC(PciDeviceConstants.PIC),
    SMBus(PciDeviceConstants.SMBus),
    Generic(PciDeviceConstants.Generic),
    Custom(PciDeviceConstants.Custom);

    private String value;

    PciDeviceType(String value) {
        this.value = value;
    }

    public boolean isEqual(String str) {
        return this.value.equals(str);
    }

    public static List<PciDeviceType> leagalPciDeviceCandidateTypes = asList(
            GPU_Video_Controller,
            GPU_3D_Controller,
            Ethernet_Controller,
            Audio_Controller,
            USB_Controller,
            Moxa_Device,
            Generic,
            Custom
    );

    public static List<PciDeviceType> enabledPassThroughPciDeviceTypes = asList(
            GPU_Video_Controller,
            GPU_Audio_Controller,
            GPU_USB_Controller,
            GPU_Serial_Controller,
            GPU_3D_Controller
    );

    public static List<PciDeviceType> disabledPassThroughPciDeviceTypes = asList(
            RAID_Controller,
            SATA_Controller,
            Memory_Controller,
            Non_Volatile_Memory_Controller,
            Fibre_Channel,
            System_Peripheral,
            ISA_Bridge,
            PCI_Bridge,
            Host_Bridge,
            Performance_Counters,
            Signal_Processing_Controller,
            Serial_Controller,
            Communication_Controller,
            PIC,
            SMBus
    );
}
