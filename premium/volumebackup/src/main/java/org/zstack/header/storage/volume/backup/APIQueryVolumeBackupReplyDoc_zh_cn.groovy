package org.zstack.header.storage.volume.backup

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.backup.VolumeBackupInventory

doc {

	title "查询卷备份结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.volume.backup.APIQueryVolumeBackupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_2.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.storage.volume.backup.APIQueryVolumeBackupReply.inventories"
		desc "卷备份清单列表"
		type "List"
		since "zsv_0.6"
		clz VolumeBackupInventory.class
	}
}
