package org.zstack.billing.table

import org.zstack.header.errorcode.ErrorCode
import org.zstack.billing.table.AccountPriceTableRefInventory

doc {

	title "查询账号价目表关联关系返回值"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.billing.table.APIQueryAccountPriceTableRefReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.billing.table.APIQueryAccountPriceTableRefReply.inventories"
		desc "null"
		type "List"
		since "3.7"
		clz AccountPriceTableRefInventory.class
	}
}
