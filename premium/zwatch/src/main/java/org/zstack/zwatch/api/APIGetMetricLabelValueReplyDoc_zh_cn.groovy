package org.zstack.zwatch.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取监控项标签值返回"

	ref {
		name "error"
		path "org.zstack.zwatch.api.APIGetMetricLabelValueReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	field {
		name "labels"
		desc "标签值"
		type "List"
		since "0.6"
	}
}
