package org.zstack.network.service.vip

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取VIP空闲端口"

	field {
		name "availablePort"
		desc "空闲端口列表"
		type "List"
		since "3.17.21"
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.21"
	}
	ref {
		name "error"
		path "org.zstack.network.service.vip.APIGetVipAvailablePortReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.21"
		clz ErrorCode.class
	}
}
