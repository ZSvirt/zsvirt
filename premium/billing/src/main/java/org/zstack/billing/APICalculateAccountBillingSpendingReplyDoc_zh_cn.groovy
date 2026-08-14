package org.zstack.billing

import org.zstack.header.errorcode.ErrorCode
import org.zstack.billing.Spending
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询账户花费返回值"

	ref {
		name "error"
		path "org.zstack.billing.APICalculateAccountBillingSpendingReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7"
		clz ErrorCode.class
	}
	field {
		name "total"
		desc ""
		type "double"
		since "3.7"
	}
	ref {
		name "spending"
		path "org.zstack.billing.APICalculateAccountBillingSpendingReply.spending"
		desc "null"
		type "List"
		since "3.7"
		clz Spending.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.7"
	}
}
