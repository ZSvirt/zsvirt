package org.zstack.header.network.l3

import org.zstack.header.errorcode.ErrorCode

doc {

	title "网络类型清单"

	ref {
		name "error"
		path "org.zstack.header.network.l3.APIGetL3NetworkTypesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_0.6"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	field {
		name "types"
		desc ""
		type "List"
		since "zsv_0.6"
	}
}
