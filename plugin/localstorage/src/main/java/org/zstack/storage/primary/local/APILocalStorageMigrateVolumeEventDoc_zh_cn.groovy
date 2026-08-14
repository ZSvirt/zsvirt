package org.zstack.storage.primary.local

import org.zstack.header.errorcode.ErrorCode

doc {

	title "本地存储资源引用清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.primary.local.APILocalStorageMigrateVolumeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.primary.local.APILocalStorageMigrateVolumeEvent.inventory"
		desc "null"
		type "LocalStorageResourceRefInventory"
		since "0.6"
		clz LocalStorageResourceRefInventory.class
	}
}
