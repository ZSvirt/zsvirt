package org.zstack.header.storage.backup

import org.zstack.header.core.trash.InstallPathRecycleInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取备份存储上的回收数据列表"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.backup.APIGetTrashOnBackupStorageReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_3.2.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.storage.backup.APIGetTrashOnBackupStorageReply.inventories"
		desc "回收数据清单"
		type "List"
		since "zsv_3.7.0"
		clz InstallPathRecycleInventory.class
	}
}
