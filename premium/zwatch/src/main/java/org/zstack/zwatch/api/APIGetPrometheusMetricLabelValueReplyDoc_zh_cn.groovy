package org.zstack.zwatch.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取指定标签的监控项数据结果"

	field {
		name "labelValues"
		desc "标签值"
		type "Map"
		since "4.10.20"
	}
	field {
		name "success"
		desc "获取操作是否成功"
		type "boolean"
		since "4.10.20"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.api.APIGetPrometheusMetricLabelValueReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.20"
		clz ErrorCode.class
	}
}
