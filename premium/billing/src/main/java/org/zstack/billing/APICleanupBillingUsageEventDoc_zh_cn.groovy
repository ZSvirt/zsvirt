package org.zstack.billing

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除全部计费资源打点数据结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.billing.APICleanupBillingUsageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.10"
		clz ErrorCode.class
	}
}
