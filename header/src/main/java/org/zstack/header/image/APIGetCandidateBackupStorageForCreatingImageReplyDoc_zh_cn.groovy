package org.zstack.header.image

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.backup.BackupStorageInventory

doc {

	title "创建镜像的备份存储候选"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.image.APIGetCandidateBackupStorageForCreatingImageReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.image.APIGetCandidateBackupStorageForCreatingImageReply.inventories"
		desc "null"
		type "List"
		since "zsv 0.6"
		clz BackupStorageInventory.class
	}
}
