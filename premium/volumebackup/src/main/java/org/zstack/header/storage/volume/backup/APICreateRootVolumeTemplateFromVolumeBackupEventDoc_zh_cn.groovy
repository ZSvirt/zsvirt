package org.zstack.header.storage.volume.backup

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.image.ImageInventory

doc {

	title "从卷备份创建根云盘镜像"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.volume.backup.APICreateRootVolumeTemplateFromVolumeBackupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.storage.volume.backup.APICreateRootVolumeTemplateFromVolumeBackupEvent.inventory"
		desc "null"
		type "ImageInventory"
		since "2.6"
		clz ImageInventory.class
	}
}
