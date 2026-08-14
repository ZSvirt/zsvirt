package org.zstack.compute.vm;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class VmHardwareSystemTags {
    // cpu topology
    public static String CPU_SOCKETS_TOKEN = "cpuSockets";
    public static PatternedSystemTag CPU_SOCKETS = new PatternedSystemTag(
            String.format("cpuSockets::{%s}", CPU_SOCKETS_TOKEN), VmInstanceVO.class);
    public static String CPU_CORES_TOKEN = "cpuCores";
    public static PatternedSystemTag CPU_CORES = new PatternedSystemTag(
            String.format("cpuCores::{%s}", CPU_CORES_TOKEN), VmInstanceVO.class);
    public static String CPU_THREADS_TOKEN = "cpuThreads";
    public static PatternedSystemTag CPU_THREADS = new PatternedSystemTag(
            String.format("cpuThreads::{%s}", CPU_THREADS_TOKEN), VmInstanceVO.class);
}
