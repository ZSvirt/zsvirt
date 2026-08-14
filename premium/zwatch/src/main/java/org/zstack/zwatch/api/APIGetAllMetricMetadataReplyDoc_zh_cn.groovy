package org.zstack.zwatch.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.api.APIGetAllMetricMetadataReply.MetricStruct

doc {

	title "获取所有监控项元数据返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.api.APIGetAllMetricMetadataReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "metrics"
		path "org.zstack.zwatch.api.APIGetAllMetricMetadataReply.metrics"
		desc "null"
		type "List"
		since "2.3"
		clz MetricStruct.class
	}
}
