package org.zstack.compute.vm;

import org.zstack.header.tag.AdminOnlyTag;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class MevocoVmSystemTags {

    public static String VM_EMULATOR_PINNING_TOKEN = "vmEmulatorPinning";
    public static PatternedSystemTag VM_EMULATOR_PINNING = new PatternedSystemTag(String.format("vmEmulatorPinning::{%s}", VM_EMULATOR_PINNING_TOKEN), VmInstanceVO.class);

    public static String VM_NUMA_ENABLE_TOKEN = "vmNumaEnable";
    /**
     * Only support "vmNumaEnable::true".
     * "vmNumaEnable::false" is invalid.
     */
    @AdminOnlyTag
    public static PatternedSystemTag VM_NUMA_ENABLE = new PatternedSystemTag(String.format("vmNumaEnable::{%s}", VM_NUMA_ENABLE_TOKEN),VmInstanceVO.class);

    public static String VM_CPU_PINNING_TOKEN = "vmCpuPinning";
    @AdminOnlyTag
    public static PatternedSystemTag VM_CPU_PINNING = new PatternedSystemTag(String.format("vmCpuPinning::{%s}", VM_CPU_PINNING_TOKEN), VmInstanceVO.class);

    public static String SECURITY_LEVEL_TOKEN = "securityLevel";
    public static PatternedSystemTag SECURITY_LEVEL =
            new PatternedSystemTag(String.format("securityLevel::{%s}", SECURITY_LEVEL_TOKEN), VmInstanceVO.class);
}
