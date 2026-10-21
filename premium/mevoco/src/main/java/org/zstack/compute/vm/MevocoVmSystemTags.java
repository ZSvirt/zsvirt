package org.zstack.compute.vm;

import org.zstack.header.tag.AdminOnlyTag;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.SystemTag;

@TagDefinition
public class MevocoVmSystemTags {

    public static String VM_EMULATOR_PINNING_TOKEN = "vmEmulatorPinning";
    public static SystemTag VM_EMULATOR_PINNING = new SystemTag(String.format("vmEmulatorPinning::{%s}", VM_EMULATOR_PINNING_TOKEN), VmInstanceVO.class);

    public static String VM_NUMA_ENABLE_TOKEN = "vmNumaEnable";
    /**
     * Only support "vmNumaEnable::true".
     * "vmNumaEnable::false" is invalid.
     */
    @AdminOnlyTag
    public static SystemTag VM_NUMA_ENABLE = new SystemTag(String.format("vmNumaEnable::{%s}", VM_NUMA_ENABLE_TOKEN),VmInstanceVO.class);

    public static String VM_CPU_PINNING_TOKEN = "vmCpuPinning";
    @AdminOnlyTag
    public static SystemTag VM_CPU_PINNING = new SystemTag(String.format("vmCpuPinning::{%s}", VM_CPU_PINNING_TOKEN), VmInstanceVO.class);

    public static String SECURITY_LEVEL_TOKEN = "securityLevel";
    public static SystemTag SECURITY_LEVEL =
            new SystemTag(String.format("securityLevel::{%s}", SECURITY_LEVEL_TOKEN), VmInstanceVO.class);
}
