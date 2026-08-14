package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode

doc {

	title "物理网卡定位结果"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.12.0"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APILocateHostNetworkInterfaceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.12.0"
		clz ErrorCode.class
	}
}
