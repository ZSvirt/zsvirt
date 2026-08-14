package org.zstack.header.host

import org.zstack.header.host.BlockDevices.BlockDevice
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取物理的磁盘设备信息返回"

	ref {
		name "blockDevices"
		path "org.zstack.header.host.APIGetHostBlockDevicesReply.blockDevices"
		desc "null"
		type "List"
		since "zsv 4.10.0"
		clz BlockDevice.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIGetHostBlockDevicesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.10.0"
		clz ErrorCode.class
	}
}
