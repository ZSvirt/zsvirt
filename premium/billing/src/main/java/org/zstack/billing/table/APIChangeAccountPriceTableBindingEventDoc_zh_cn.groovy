package org.zstack.billing.table

import org.zstack.header.errorcode.ErrorCode
import org.zstack.billing.table.PriceTableInventory

doc {

	title "修改账号计费价目返回值"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.billing.table.APIChangeAccountPriceTableBindingEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.billing.table.APIChangeAccountPriceTableBindingEvent.inventory"
		desc "null"
		type "PriceTableInventory"
		since "3.7"
		clz PriceTableInventory.class
	}
}
