package org.zstack.billing.table

import org.zstack.header.errorcode.ErrorCode
import org.zstack.billing.table.PriceTableInventory

doc {

	title "查询计费价目表返回值"

	ref {
		name "error"
		path "org.zstack.billing.table.APIQueryPriceTableRely.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.billing.table.APIQueryPriceTableRely.inventories"
		desc "null"
		type "List"
		since "3.7"
		clz PriceTableInventory.class
	}
}
