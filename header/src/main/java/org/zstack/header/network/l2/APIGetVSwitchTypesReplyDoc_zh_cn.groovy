package org.zstack.header.network.l2

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取虚拟交换机类型的请求返回"

	ref {
		name "error"
		path "org.zstack.header.network.l2.APIGetVSwitchTypesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.11.0"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 0.6"
	}
	field {
		name "types"
		desc "虚拟交换机类型列表"
		type "List"
		since "zsv 3.11.0"
	}
}