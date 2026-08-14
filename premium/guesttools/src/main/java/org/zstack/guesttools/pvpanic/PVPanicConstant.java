package org.zstack.guesttools.pvpanic;

import org.zstack.header.configuration.PythonClass;

/**
 * Created by Wenhao.Zhang on 21/07/19
 */
@PythonClass
public interface PVPanicConstant {
    /**
     * For Param of Start VM Command
     */
    String PANIC_ISA = "panicIsa";
    String PANIC_HYPERV = "panicHyperv";

    /**
     * For Boot Param of Guest Tools
     */
    String PVPANIC = "pvpanic";

    String PVPANIC_HOST = "pvpanic_host_enable";
    String PVPANIC_GUEST_TOOL = "pvpanic_guest_tools_enable";
    String PVPANIC_KERNEL = "pvpanic_guest_kernel_supported";

    String RESULT_ENABLE = "enable";
    String RESULT_DISABLE = "disable";
    String RESULT_SUPPORTED = "supported";
    String RESULT_NOT_SUPPORT = "not supported";
    String RESULT_UNKNOWN = "unknown";

    /**
     * For PVPanic Host
     */
    String HOST_ENABLE = RESULT_ENABLE;
    String HOST_DISABLE = RESULT_DISABLE;
    String HOST_UNKNOWN = RESULT_UNKNOWN;

    /**
     * For PVPanic Guest Tool
     */
    String GUEST_TOOL_ENABLE = RESULT_ENABLE;
    String GUEST_TOOL_DISABLE = RESULT_DISABLE;
    String GUEST_TOOL_NOT_SUPPORT = RESULT_NOT_SUPPORT;
    String GUEST_TOOL_UNKNOWN = RESULT_UNKNOWN;

    /**
     * For PVPanic Kernel
     */
    String KERNEL_UNKNOWN = RESULT_UNKNOWN;
    String KERNEL_NOT_SUPPORT = RESULT_NOT_SUPPORT;
    String KERNEL_SUPPORTED = RESULT_SUPPORTED;

    /**
     * For VM Boot Param
     */
    String VM_BOOT_PARAM_ON_CRASH = "onCrash";

    /**
     * Configuration files
     */
    String FEATURE_VERSION_FILE_PATH = "guestTools/feature.yaml";
}
