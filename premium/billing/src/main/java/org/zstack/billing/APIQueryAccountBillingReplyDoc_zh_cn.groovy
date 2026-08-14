package org.zstack.billing

import org.zstack.header.errorcode.ErrorCode
import org.zstack.billing.generator.BillingInventory

doc {

	title "查询账户账单返回值"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.billing.APIQueryAccountBillingReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.billing.APIQueryAccountBillingReply.inventories"
		desc "null"
		type "List"
		since "3.7"
		clz BillingInventory.class
	}
}
