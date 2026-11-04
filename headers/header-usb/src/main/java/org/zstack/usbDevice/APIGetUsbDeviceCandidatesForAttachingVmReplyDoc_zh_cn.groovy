package org.zstack.usbDevice

import org.zstack.header.errorcode.ErrorCode
import org.zstack.usbDevice.entity.UsbDeviceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "USB透传候选列表"

	ref {
		name "error"
		path "org.zstack.usbDevice.APIGetUsbDeviceCandidatesForAttachingVmReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_2.2"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.usbDevice.APIGetUsbDeviceCandidatesForAttachingVmReply.inventories"
		desc "USB设备"
		type "List"
		since "zsv_2.2"
		clz UsbDeviceInventory.class
	}
	field {
		name "success"
		desc "成功"
		type "boolean"
		since "zsv_2.2"
	}
}
