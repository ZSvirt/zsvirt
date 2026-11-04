package org.zstack.compute.vm;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.SystemTag;

@TagDefinition
public class VmHardwareSystemTags {
    // cpu topology
    public static String CPU_SOCKETS_TOKEN = "cpuSockets";
    public static SystemTag CPU_SOCKETS = new SystemTag(
            String.format("cpuSockets::{%s}", CPU_SOCKETS_TOKEN), VmInstanceVO.class);
    public static String CPU_CORES_TOKEN = "cpuCores";
    public static SystemTag CPU_CORES = new SystemTag(
            String.format("cpuCores::{%s}", CPU_CORES_TOKEN), VmInstanceVO.class);
    public static String CPU_THREADS_TOKEN = "cpuThreads";
    public static SystemTag CPU_THREADS = new SystemTag(
            String.format("cpuThreads::{%s}", CPU_THREADS_TOKEN), VmInstanceVO.class);
}
