package org.zstack.header.network.service

import org.zstack.header.errorcode.ErrorCode

doc {

	title "网络服务类型清单"

	ref {
		name "error"
		path "org.zstack.header.network.service.APIGetNetworkServiceTypesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	field {
		name "types"
		desc ""
		type "Map"
		since "zsv 0.6"
	}
}
