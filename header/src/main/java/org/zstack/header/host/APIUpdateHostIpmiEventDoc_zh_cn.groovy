package org.zstack.header.host

import org.zstack.header.host.HostIpmiInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新物理机 IPMI 信息消息回复"

	ref {
		name "hostIpmiInventory"
		path "org.zstack.header.host.APIUpdateHostIpmiEvent.hostIpmiInventory"
		desc "物理机IPMI信息"
		type "HostIpmiInventory"
		since "zsv 3.17.0"
		clz HostIpmiInventory.class
	}
	field {
		name "success"
		desc "是否成功"
		type "boolean"
		since "zsv 3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIUpdateHostIpmiEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.17.0"
		clz ErrorCode.class
	}
}
