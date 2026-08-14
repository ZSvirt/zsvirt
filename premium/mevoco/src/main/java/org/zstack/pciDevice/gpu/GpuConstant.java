package org.zstack.pciDevice.gpu;

import org.zstack.pciDevice.PciDeviceType;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/7 13:27
 */
public class GpuConstant {

    public static final String GPU_DEVICE_TYPE = "GPU";

    public static final String GPU_MEMORY_NAME = "memory";
    public static final String GPU_POWER_NAME = "power";
    public static final String GPU_SERIALNUMBER_NAME = "serialNumber";
    public static final String GPU_IS_DRIVER_LOADED_NAME = "isDriverLoaded";

    public static List<PciDeviceType> gpuPciDeviceTypes = asList(
            PciDeviceType.GPU_Video_Controller,
            PciDeviceType.GPU_3D_Controller
    );
}
