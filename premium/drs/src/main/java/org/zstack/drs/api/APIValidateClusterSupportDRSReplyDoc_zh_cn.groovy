package org.zstack.drs.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查看集群是否支持DRS的结果"

	field {
		name "supported"
		desc "集群是否支持 DRS"
		type "boolean"
		since "4.0.0"
	}
	ref {
		name "reason"
		path "org.zstack.drs.api.APIValidateClusterSupportDRSReply.reason"
		desc "错误码，如果集群不支持DRS，这里会罗列至少一个不支持的理由"
		type "ErrorCode"
		since "4.0.0"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc "查看任务是否成功"
		type "boolean"
		since "4.0.0"
	}
	ref {
		name "error"
		path "org.zstack.drs.api.APIValidateClusterSupportDRSReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.0.0"
		clz ErrorCode.class
	}
}
