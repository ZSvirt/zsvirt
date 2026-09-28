package org.zstack.usbDevice

import org.zstack.header.errorcode.ErrorCode
import org.zstack.usbDevice.entity.UsbDeviceInventory

doc {

	title "USB卸载结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.usbDevice.APIDetachUsbDeviceFromVmEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_2.2"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.usbDevice.APIDetachUsbDeviceFromVmEvent.inventory"
		desc "USB设备"
		type "UsbDeviceInventory"
		since "zsv_2.2"
		clz UsbDeviceInventory.class
	}
}
