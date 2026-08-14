package org.zstack.mttyDevice

import org.zstack.header.errorcode.ErrorCode

doc {

	title "MTTY 设备虚拟化还原结果"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.15.11"
	}
	ref {
		name "error"
		path "org.zstack.mttyDevice.APIUngenerateSeMdevDevicesEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.15.11"
		clz ErrorCode.class
	}
}
