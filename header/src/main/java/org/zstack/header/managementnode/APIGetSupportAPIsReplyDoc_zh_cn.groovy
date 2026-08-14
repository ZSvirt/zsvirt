package org.zstack.header.managementnode

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取支持的 API 列表的请求返回"

	field {
		name "supportApis"
		desc "支持的 API 列表"
		type "List"
		since "zsv 3.15.0"
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.15.0"
	}
	ref {
		name "error"
		path "org.zstack.header.managementnode.APIGetSupportAPIsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.15.0"
		clz ErrorCode.class
	}
}
