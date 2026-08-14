package org.zstack.managements.api.ha2

import org.zstack.header.errorcode.ErrorCode

doc {

	title "降级双管中当前管理节点操作的结果"

	field {
		name "success"
		desc "降级操作是否成功"
		type "boolean"
		since "4.10.20"
	}
	ref {
		name "error"
		path "org.zstack.managements.api.ha2.APIZSha2DemoteEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.20"
		clz ErrorCode.class
	}
}
