package org.zstack.pciDevice.gpu;

import org.zstack.pciDevice.PciDeviceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/7 13:19
 */
@StaticMetamodel(GpuDeviceVO.class)
public class GpuDeviceVO_ extends PciDeviceVO_ {
    public static volatile SingularAttribute<GpuDeviceVO, String> serialNumber;
    public static volatile SingularAttribute<GpuDeviceVO, Long> memory;
    public static volatile SingularAttribute<GpuDeviceVO, Long> power;
    public static volatile SingularAttribute<GpuDeviceVO, Boolean> isDriverLoaded;
}
