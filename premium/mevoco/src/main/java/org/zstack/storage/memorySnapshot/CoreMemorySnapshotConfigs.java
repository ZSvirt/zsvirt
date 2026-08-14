package org.zstack.storage.memorySnapshot;

import org.zstack.compute.vm.MevocoVmSystemTags;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.VmHardwareSystemTags;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.config.GlobalConfig;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.pciDevice.PciDeviceGlobalConfig;
import org.zstack.resourceconfig.BindResourceConfig;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTag;
import org.zstack.utils.BeanUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CoreMemorySnapshotConfigs implements ManagementNodeReadyExtensionPoint {
    public static final List<PatternedSystemTag> restoreCandidatePatternedSystemTags = Stream.of(
            // vm
            VmSystemTags.USERDATA, VmSystemTags.USB_REDIRECT, VmSystemTags.SSHKEY, VmSystemTags.BOOT_MODE, VmSystemTags.CLEAN_TRAFFIC,
            VmSystemTags.HOSTNAME, MevocoVmSystemTags.VM_CPU_PINNING, MevocoVmSystemTags.VM_NUMA_ENABLE, VmHardwareSystemTags.CPU_CORES,
            VmSystemTags.VM_GUEST_TOOLS, VmSystemTags.MACHINE_TYPE
    ).collect(Collectors.toList());

    public static final List<SystemTag> restoreCandidateSystemTags = Stream.of(
            // vm
            VmSystemTags.VIRTIO,

            // volume
            KVMSystemTags.VOLUME_VIRTIO_SCSI, KVMSystemTags.VOLUME_SCSI
    ).collect(Collectors.toList());

    public static final List<GlobalConfig> vmRestoreCandidateConfigs = Stream.of(
            KVMGlobalConfig.NESTED_VIRTUALIZATION, VmGlobalConfig.VM_SPICE_STREAMING_MODE,
            KVMGlobalConfig.VM_CPU_QUOTA, VmGlobalConfig.VM_CLOCK_TRACK,
            VmGlobalConfig.VM_BOOT_MENU_SPLASH_TIMEOUT, VmGlobalConfig.KVM_HIDDEN_STATE,
            VmGlobalConfig.VM_PORT_OFF, VmGlobalConfig.EMULATE_HYPERV,
            PciDeviceGlobalConfig.ENABLE_HOT_PLUG, KVMGlobalConfig.VM_CPU_HYPERVISOR_FEATURE,
            VmGlobalConfig.VM_SOUND_TYPE, VmGlobalConfig.VM_VIDEO_TYPE,
            VmGlobalConfig.NUMA, VmGlobalConfig.RESET_TPM_AFTER_VM_CLONE,
            KVMGlobalConfig.VM_EDK_VERSION_CONFIG,
            VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT
    ).collect(Collectors.toList());

    public static final List<GlobalConfig> volumeRestoreCandidateConfigs = Stream.of(
            KVMGlobalConfig.LIBVIRT_CACHE_MODE, MevocoGlobalConfig.AIO_NATIVE
    ).collect(Collectors.toList());

    public static final List<GlobalConfig> vmNicRestoreCandidateConfigs = Stream.of(
            VmGlobalConfig.VM_NIC_MULTIQUEUE_NUM
    ).collect(Collectors.toList());

    @Override
    public void managementNodeReady() {
        populateRestoreCandidateConfigsFromAnnotations();
    }

    public static void populateRestoreCandidateConfigsFromAnnotations() {
        BeanUtils.reflections.getFieldsAnnotatedWith(NeedRestoreOnVmApplySnapshot.class).forEach(field -> {
            try {
                GlobalConfig config = (GlobalConfig) field.get(null);
                if (config == null) {
                    throw new CloudRuntimeException(String.format("failed to retrieve GlobalConfig from the field[%s] annotated with NeedRestoreOnVmApplySnapshot", field));
                }

                BindResourceConfig bindResourceConfig = field.getAnnotation(BindResourceConfig.class);
                if (bindResourceConfig == null) {
                    throw new CloudRuntimeException(String.format("@NeedRestoreOnVmApplySnapshot on field [%s] requires @BindResourceConfig annotation", field.getName()));
                }

                List<String> bindResourceConfigs = Arrays.stream(bindResourceConfig.value()).map(Class::getName).collect(Collectors.toList());
                if (bindResourceConfigs.contains(VmInstanceVO.class.getName())) {
                    vmRestoreCandidateConfigs.add(config);
                }
                if (bindResourceConfigs.contains(VolumeVO.class.getName())) {
                    volumeRestoreCandidateConfigs.add(config);
                }
                if (bindResourceConfigs.contains(VmNicVO.class.getName())) {
                    vmNicRestoreCandidateConfigs.add(config);
                }
            } catch (IllegalAccessException e) {
                throw new CloudRuntimeException(e);
            }
        });
    }

    public static List<GlobalConfig> getRestoreCandidateConfigs() {
        List<GlobalConfig> allConfigs = new ArrayList<>();
        allConfigs.addAll(vmRestoreCandidateConfigs);
        allConfigs.addAll(volumeRestoreCandidateConfigs);
        allConfigs.addAll(vmNicRestoreCandidateConfigs);
        return allConfigs;
    }

    public static List<String> getRestoreCandidateConfigsIdentity() {
        return getRestoreCandidateConfigs().stream().map(GlobalConfig::getIdentity).collect(Collectors.toList());
    }

    public static boolean systemTagIsRegistered(String configName) {
        for (PatternedSystemTag tag : restoreCandidatePatternedSystemTags) {
            if (Objects.equals(tag.getTagFormat(), configName)) {
                return true;
            }
        }

        for (SystemTag tag : restoreCandidateSystemTags) {
            if (Objects.equals(tag.getTagFormat(), configName)) {
                return true;
            }
        }

        return false;
    }

    public static boolean resourceConfigIsRegistered(String targetIdentity) {
        for (String identity : getRestoreCandidateConfigsIdentity()) {
            if (Objects.equals(identity, targetIdentity)) {
                return true;
            }
        }
        return false;
    }
}
