package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取云主机第一启动项的返回"

	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmInstanceFirstBootDeviceReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7.0"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	field {
		name "firstBootDevice"
		desc "云主机第一启动项"
		type "String"
		since "3.7.0"
	}
}
