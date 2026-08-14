package org.zstack.usbDevice;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;

/**
 * @Author: DaoDao
 * @Date: 2023/9/6
 */
public class UsbDeviceUtils {

    public static ErrorCode checkNumberOfUsbDeviceAlreadyAttachedTOVm (String usbVersion, String vmUuid) {
        // attach at most 1 USB 1.0 devices to one VM
        if (usbVersion.startsWith("1")) {
            long count1 = Q.New(UsbDeviceVO.class)
                    .eq(UsbDeviceVO_.vmInstanceUuid, vmUuid)
                    .like(UsbDeviceVO_.usbVersion, "1%")
                    .count();
            if (count1 == UsbDeviceConstants.MAX_USB_1_DEVICE_PER_VM) {
                return Platform.operr(
                        "You can attach at most %s USB 1.0 devices to one vm instance.",
                        UsbDeviceConstants.MAX_USB_1_DEVICE_PER_VM
                );
            }
        }

        // attach at most 6 USB 2.0 devices to one VM
        if (usbVersion.startsWith("2")) {
            long count2 = Q.New(UsbDeviceVO.class)
                    .eq(UsbDeviceVO_.vmInstanceUuid, vmUuid)
                    .like(UsbDeviceVO_.usbVersion, "2%")
                    .count();
            if (count2 == UsbDeviceConstants.MAX_USB_2_DEVICE_PER_VM) {
                return Platform.operr(
                        "You can attach at most %s USB 2.0 devices to one vm instance.",
                        UsbDeviceConstants.MAX_USB_2_DEVICE_PER_VM
                );
            }
        }

        // attach at most 4 USB 3.0 devices to one VM
        if (usbVersion.startsWith("3")) {
            long count3 = Q.New(UsbDeviceVO.class)
                    .eq(UsbDeviceVO_.vmInstanceUuid, vmUuid)
                    .like(UsbDeviceVO_.usbVersion, "3%")
                    .count();
            if (count3 == UsbDeviceConstants.MAX_USB_3_DEVICE_PER_VM) {
                return Platform.operr(
                        "You can attach at most %s USB 3.0 devices to one vm instance.",
                        UsbDeviceConstants.MAX_USB_3_DEVICE_PER_VM
                );
            }
        }

        return null;
    }
}
