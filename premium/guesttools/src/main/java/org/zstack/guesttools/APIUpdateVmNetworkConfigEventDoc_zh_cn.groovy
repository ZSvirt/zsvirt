package org.zstack.guesttools

import org.zstack.header.errorcode.ErrorCode

doc {

	title "同步虚拟机网络配置的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.16.11"
	}
	ref {
		name "error"
		path "org.zstack.guesttools.APIUpdateVmNetworkConfigEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.11"
		clz ErrorCode.class
	}
}
