package org.zstack.header.identity

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.identity.Quota.QuotaUsage

doc {

	title "账户配额使用情况列表"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.identity.APIGetAccountQuotaUsageReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	ref {
		name "usages"
		path "org.zstack.header.identity.APIGetAccountQuotaUsageReply.usages"
		desc "账户配额使用情况列表"
		type "List"
		since "zsv 0.6"
		clz QuotaUsage.class
	}
}
