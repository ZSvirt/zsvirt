package org.zstack.storage.migration.primary

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.volume.VolumeInventory

doc {

	title "跨存储迁移云盘结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVolumeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.2"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVolumeEvent.inventory"
		desc "跨存储迁移所得云盘"
		type "VolumeInventory"
		since "2.2"
		clz VolumeInventory.class
	}
}
