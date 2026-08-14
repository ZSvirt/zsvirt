package org.zstack.drs.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除集群 DRS 的结果"

	field {
		name "success"
		desc "删除是否成功"
		type "boolean"
		since "3.10.0"
	}
	ref {
		name "error"
		path "org.zstack.drs.api.APIDeleteClusterDRSEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.10.0"
		clz ErrorCode.class
	}
}
