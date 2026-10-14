package org.zstack.pciDevice.spi;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HypervisorType;
import org.zstack.pciDevice.entity.PciDeviceInventory;
import org.zstack.pciDevice.entity.specification.mdev.MdevDeviceSpecInventory;
import org.zstack.pciDevice.entity.specification.pci.PciDeviceSpecInventory;

import java.util.List;

/**
 * Created by weiwang on 10/07/2017.
 *
 * To ZSTAC-83192:
 * * PciDeviceBackend is a backend to KVM HOST! not a place to set method like 'syncPciDeviceInDb';
 */
public interface PciDeviceBackend {
    void attachPciDeviceToVm(PciDeviceInventory pciDevice, String vmUuid, Completion completion);
    void detachPciDeviceFromVm(PciDeviceInventory pciDevice, String vmUuid, Completion completion);
    void attachPciDeviceToHost(PciDeviceInventory pciDevice, Completion completion);
    void detachPciDeviceFromHost(PciDeviceInventory pciDevice, Completion completion);
    void syncPciDeviceFromHost(String hostUuid, List<String> pciDeviceAddresses, String hostIommuState, boolean skipGrubConfig, Completion completion);
    void createPciDeviceRomFileInHost(PciDeviceSpecInventory spec, String hostUuid, Completion completion);
    void generateSriovPciDevices(String hostUuid, PciDeviceInventory pciDevice, Integer virtPartNum, Boolean reSplite, Completion completion);
    void ungenerateSriovPciDevices(String hostUuid, PciDeviceInventory pciDevice, Completion completion);
    void generateVfioMdevDevices(String hostUuid, PciDeviceInventory pciDevice, MdevDeviceSpecInventory mdevSpec, List<String> mdevUuids, Completion completion);
    void unGenerateVfioMdevDevices(String hostUuid, PciDeviceInventory pciDevice, MdevDeviceSpecInventory mdevSpec, List<String> mdevUuids, Completion completion);

    HypervisorType getHypervisorType();
}
