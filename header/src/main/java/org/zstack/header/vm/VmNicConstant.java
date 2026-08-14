package org.zstack.header.vm;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface VmNicConstant {
    public static final String NIC_DRIVER_TYPE_VIRTIO = "virtio";
    public static final String NIC_DRIVER_TYPE_E1000 = "e1000";
    public static final String NIC_DRIVER_TYPE_RTL8139 = "rtl8139";
    public static final String NIC_DRIVER_TYPE_PCNET = "pcnet";
    public static final String NIC_DRIVER_TYPE_SR_IOV = "SR-IOV";
}
