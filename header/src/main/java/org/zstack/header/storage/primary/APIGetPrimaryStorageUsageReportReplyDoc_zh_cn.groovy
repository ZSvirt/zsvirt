package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.UsageReport
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取主存储预测容量报告返回"

	ref {
		name "uriUsageForecast"
		path "org.zstack.header.storage.primary.APIGetPrimaryStorageUsageReportReply.uriUsageForecast"
		desc "预测容量表"
		type "Map"
		since "zsv 3.17.21"
		clz UsageReport.class
	}
	ref {
		name "usageReport"
		path "org.zstack.header.storage.primary.APIGetPrimaryStorageUsageReportReply.usageReport"
		desc "使用容量报告"
		type "UsageReport"
		since "zsv 3.17.21"
		clz UsageReport.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.17.21"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.primary.APIGetPrimaryStorageUsageReportReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.17.21"
		clz ErrorCode.class
	}
}
