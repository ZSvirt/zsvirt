package org.zstack.storage.migration.primary

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostInventory

doc {

	title "跨存储迁移可选主机列表"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.10.0"
	}
	ref {
		name "error"
		path "org.zstack.storage.migration.primary.APIGetHostCandidatesForVmMigrationReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.storage.migration.primary.APIGetHostCandidatesForVmMigrationReply.inventories"
		desc "候选主机清单列表"
		type "List"
		since "3.10.0"
		clz HostInventory.class
	}
}
