package org.zstack.usbDevice;

import org.zstack.header.configuration.PythonClass;

/**
 * Created by GuoYi on 10/21/17.
 */
public class UsbDeviceConstants {
    @PythonClass
    public static final String SERVICE_ID = "usbDevice";
    public static final String ACTION_CATEGORY = "usbDevice";
    public static final String USB_DEVICE_ALLOCATOR_STRATEGY = "UsbDeviceAllocatorStrategy";

    public static final long MAX_USB_1_DEVICE_PER_VM = 1L;
    public static final long MAX_USB_2_DEVICE_PER_VM = 6L;
    public static final long MAX_USB_3_DEVICE_PER_VM = 4L;
}
