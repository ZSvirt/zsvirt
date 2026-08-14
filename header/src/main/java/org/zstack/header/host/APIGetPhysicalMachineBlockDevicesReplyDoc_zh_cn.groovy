package org.zstack.header.host

import org.zstack.header.host.BlockDevices
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取服务器磁盘信息返回"

	ref {
		name "blockDevices"
		path "org.zstack.header.host.APIGetPhysicalMachineBlockDevicesReply.blockDevices"
		desc "物理机磁盘信息"
		type "BlockDevices"
		since "zsv 4.3.0"
		clz BlockDevices.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.3.0"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIGetPhysicalMachineBlockDevicesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.3.0"
		clz ErrorCode.class
	}
}
