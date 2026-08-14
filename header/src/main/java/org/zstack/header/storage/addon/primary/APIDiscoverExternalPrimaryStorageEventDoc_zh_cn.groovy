package org.zstack.header.storage.addon.primary

import org.zstack.header.errorcode.ErrorCode

doc {

	title "发现外部存储结果"

	ref {
		name "inventory"
		path "org.zstack.header.storage.addon.primary.APIDiscoverExternalPrimaryStorageEvent.inventory"
		desc "外部存储清单"
		type "ExternalPrimaryStorageInventory"
		since "zsv 4.10.6"
		clz ExternalPrimaryStorageInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 4.10.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.addon.primary.APIDiscoverExternalPrimaryStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 4.10.6"
		clz ErrorCode.class
	}
}
