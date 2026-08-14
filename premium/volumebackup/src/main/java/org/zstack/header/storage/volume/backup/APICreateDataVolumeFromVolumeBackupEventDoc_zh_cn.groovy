package org.zstack.header.storage.volume.backup

import org.zstack.header.volume.VolumeInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "从备份创建数据云盘返回"

	ref {
		name "inventory"
		path "org.zstack.header.storage.volume.backup.APICreateDataVolumeFromVolumeBackupEvent.inventory"
		desc "数据云盘实例"
		type "VolumeInventory"
		since "3.18.0"
		clz VolumeInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.18.0"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.volume.backup.APICreateDataVolumeFromVolumeBackupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.18.0"
		clz ErrorCode.class
	}
}
