package org.zstack.header.identity

import org.zstack.header.errorcode.ErrorCode

doc {

	title "配额清单列表"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.identity.APIQueryQuotaReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.identity.APIQueryQuotaReply.inventories"
		desc "配额清单列表"
		type "List"
		since "zsv 0.6"
		clz QuotaInventory.class
	}
}
