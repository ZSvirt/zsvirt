package org.zstack.zwatch.api

import org.zstack.zwatch.migratedb.AuditsInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询审计数据的返回"

	ref {
		name "inventories"
		path "org.zstack.zwatch.api.APIQueryAuditReply.inventories"
		desc "审计清单返回"
		type "List"
		since "3.16.0"
		clz AuditsInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.16.0"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.api.APIQueryAuditReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.0"
		clz ErrorCode.class
	}
}
