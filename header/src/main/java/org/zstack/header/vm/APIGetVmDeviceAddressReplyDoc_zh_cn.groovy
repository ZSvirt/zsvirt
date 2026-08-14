package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取云主机设备地址结果"

	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmDeviceAddressReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.10.0"
		clz ErrorCode.class
	}
	field {
		name "addresses"
		desc "设备地址详情"
		type "Map"
		since "zsv 3.10.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 3.10.0"
	}
}
