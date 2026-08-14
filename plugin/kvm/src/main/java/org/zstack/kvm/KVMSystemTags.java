package org.zstack.kvm;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTag;

/**
 */
@TagDefinition
public class KVMSystemTags {
    public static final String QEMU_IMG_VERSION_TOKEN = "version";
    public static PatternedSystemTag QEMU_IMG_VERSION = new PatternedSystemTag(String.format("qemu-img::version::{%s}", QEMU_IMG_VERSION_TOKEN), HostVO.class);

    public static final String LIBVIRT_VERSION_TOKEN = "version";
    public static PatternedSystemTag LIBVIRT_VERSION = new PatternedSystemTag(String.format("libvirt::version::{%s}", LIBVIRT_VERSION_TOKEN), HostVO.class);

    public static final String LIBVIRT_PACKAGE_VERSION_TOKEN = "version";
    public static PatternedSystemTag LIBVIRT_PACKAGE_VERSION = new PatternedSystemTag(String.format("libvirt::package::version::{%s}", LIBVIRT_PACKAGE_VERSION_TOKEN), HostVO.class);

    public static final String HVM_CPU_FLAG_TOKEN = "flag";
    public static PatternedSystemTag HVM_CPU_FLAG = new PatternedSystemTag(String.format("hvm::{%s}", HVM_CPU_FLAG_TOKEN), HostVO.class);

    public static final String EPT_CPU_FLAG_TOKEN = "ept";
    public static PatternedSystemTag EPT_CPU_FLAG = new PatternedSystemTag(String.format("ept::{%s}", EPT_CPU_FLAG_TOKEN), HostVO.class);

    public static final String CPU_MODEL_NAME_TOKEN = "name";
    public static PatternedSystemTag CPU_MODEL_NAME = new PatternedSystemTag(String.format("cpuModelName::{%s}", CPU_MODEL_NAME_TOKEN), HostVO.class);

    public static final String CHECK_CLUSTER_CPU_MODEL_TOKEN = "enable";
    public static PatternedSystemTag CHECK_CLUSTER_CPU_MODEL = new PatternedSystemTag(String.format("check::cluster::cpu::model::{%s}", CHECK_CLUSTER_CPU_MODEL_TOKEN), ClusterVO.class);

    public static SystemTag VIRTIO_SCSI = new SystemTag("capability:virtio-scsi", HostVO.class);

    public static final String L2_BRIDGE_NAME_TOKEN = "name";
    public static PatternedSystemTag L2_BRIDGE_NAME = new PatternedSystemTag(String.format("kvm::bridge::{%s}", L2_BRIDGE_NAME_TOKEN), L2NetworkVO.class);

    public static SystemTag VOLUME_VIRTIO_SCSI = new SystemTag("capability::virtio-scsi", VolumeVO.class);

    public static SystemTag VOLUME_SCSI = new SystemTag("capability::scsi", VolumeVO.class);

    public static final String DISK_OFFERING_VIRTIO_SCSI_TOKEN = "diskOfferingUuid";
    public static final String DISK_OFFERING_VIRTIO_SCSI_NUM_TOKEN = "number";
    public static PatternedSystemTag DISK_OFFERING_VIRTIO_SCSI = new PatternedSystemTag(
            String.format("virtio::diskOffering::{%s}::num::{%s}",
                    DISK_OFFERING_VIRTIO_SCSI_TOKEN, DISK_OFFERING_VIRTIO_SCSI_NUM_TOKEN), VmInstanceVO.class);

    public static final String VOLUME_WWN_TOKEN = "wwn";
    public static PatternedSystemTag VOLUME_WWN = new PatternedSystemTag(String.format("kvm::volume::{%s}", VOLUME_WWN_TOKEN), VolumeVO.class);

    public static final String VM_PREDEFINED_PCI_BRIDGE_NUM_TOKEN = "number";
    public static PatternedSystemTag VM_PREDEFINED_PCI_BRIDGE_NUM = new PatternedSystemTag(
            String.format("vm::pci::bridge::num::{%s}",
                    VM_PREDEFINED_PCI_BRIDGE_NUM_TOKEN), VmInstanceVO.class);

    public static final String VMNIC_PCI_ADDRESS_TOKEN = "pci";
    public static PatternedSystemTag VMNIC_PCI_ADDRESS = new PatternedSystemTag(
            String.format("vmnic::{%s}", VMNIC_PCI_ADDRESS_TOKEN), VmNicVO.class);

    public static final String LIBVIRT_CAPABILITIES_TOKEN = "libvirtCapabilities";
    public static PatternedSystemTag LIBVIRT_CAPABILITIES = new PatternedSystemTag(String.format("libvirtCapabilities::{%s}", LIBVIRT_CAPABILITIES_TOKEN), HostVO.class);

    public static final String CPU_FEATURE_MD5_TOKEN = "cpuFeatureMd5";
    public static PatternedSystemTag CPU_FEATURE_MD5 = new PatternedSystemTag(String.format("cpuFeatureMd5::{%s}", CPU_FEATURE_MD5_TOKEN), HostVO.class);

    public static SystemTag FORCE_DEPLOYMENT_ONCE = new SystemTag("force::deployment::once", HostVO.class);

    public static final String EDK_RPM_TOKEN = "edkRpm";
    public static PatternedSystemTag VM_EDK =
            new PatternedSystemTag(String.format("vm::edk::{%s}", EDK_RPM_TOKEN), VmInstanceVO.class);

    public static final String SWTPM_VERSION_TOKEN = "version";
    public static PatternedSystemTag SWTPM_VERSION =
            new PatternedSystemTag(String.format("swtpm::{%s}", SWTPM_VERSION_TOKEN), HostVO.class);

    public static final String TPM_KEY_PROVIDER_NAME_TOKEN = "keyProviderName";
    public static PatternedSystemTag TPM_KEY_PROVIDER_NAME =
            new PatternedSystemTag(String.format("tpm::keyProvider::{%s}", TPM_KEY_PROVIDER_NAME_TOKEN), VmHostBackupFileVO.class);
}
