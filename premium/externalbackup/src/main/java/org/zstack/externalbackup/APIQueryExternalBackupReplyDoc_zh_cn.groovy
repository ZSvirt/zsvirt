package org.zstack.externalbackup

import org.zstack.header.errorcode.ErrorCode
import org.zstack.externalbackup.ExternalBackupInventory

doc {

	title "查询外部备份结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.externalbackup.APIQueryExternalBackupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.9.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.externalbackup.APIQueryExternalBackupReply.inventories"
		desc "null"
		type "List"
		since "3.9.0"
		clz ExternalBackupInventory.class
	}
}
