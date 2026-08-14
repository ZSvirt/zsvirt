package org.zstack.pciDevice;

import org.zstack.header.host.HostVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class PciDeviceSystemTags {
    public static final String HOST_IOMMU_STATE_TOKEN = "iommuState";
    public static final String HOST_IOMMU_STATUS_TOKEN = "iommuStatus";
    public static PatternedSystemTag HOST_IOMMU_STATE = new PatternedSystemTag(
            String.format("iommuState::{%s}", HOST_IOMMU_STATE_TOKEN), HostVO.class);
    public static PatternedSystemTag HOST_IOMMU_STATUS = new PatternedSystemTag(
            String.format("iommuStatus::{%s}", HOST_IOMMU_STATUS_TOKEN), HostVO.class);

    public static final String PCI_DEVICE_TOKEN = "pciDevice";
    // only use before vm created
    public static PatternedSystemTag PCI_DEVICE = new PatternedSystemTag(
            String.format("pciDevice::{%s}", PCI_DEVICE_TOKEN), VmInstanceVO.class);

    public static final String PCI_DEVICE_SPEC_UUID_TOKEN = "pciSpecUuid";
    public static final String PCI_DEVICE_NUMBER_TOKEN = "pciDeviceNumber";
    public static PatternedSystemTag PCI_DEVICE_SPEC = new PatternedSystemTag(
            String.format("pciDeviceSpec::{%s}::{%s}", PCI_DEVICE_SPEC_UUID_TOKEN, PCI_DEVICE_NUMBER_TOKEN), VmInstanceVO.class
    );

    public static PatternedSystemTag AUTO_RELEASE_SPEC_RELEATED_PHYSICAL_PCI_DEVICE = new PatternedSystemTag(
            "autoReleaseSpecReleatedPhysicalPciDevice", VmInstanceVO.class
    );

    public static PatternedSystemTag AUTO_RELEASE_SPEC_RELEATED_VIRTUAL_PCI_DEVICE = new PatternedSystemTag(
            "autoReleaseSpecReleatedVirtualPciDevice", VmInstanceVO.class
    );

    public static final String VM_INSTANCE_UUID_TOKEN = "vmInstanceUuid";
    public static final String L3_NETWORK_UUID_TOKEN = "l3NetworkUuid";
    public static PatternedSystemTag PCI_DEVICE_AS_VF_NIC = new PatternedSystemTag(
            String.format("pciDeviceAsVfNic::{%s}::{%s}", VM_INSTANCE_UUID_TOKEN, L3_NETWORK_UUID_TOKEN), PciDeviceVO.class);
}
