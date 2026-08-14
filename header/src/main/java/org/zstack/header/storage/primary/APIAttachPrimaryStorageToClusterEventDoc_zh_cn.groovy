package org.zstack.header.storage.primary

import org.zstack.header.errorcode.ErrorCode

doc {

	title "主存储清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.primary.APIAttachPrimaryStorageToClusterEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.storage.primary.APIAttachPrimaryStorageToClusterEvent.inventory"
		desc "null"
		type "PrimaryStorageInventory"
		since "zsv 0.6"
		clz PrimaryStorageInventory.class
	}
}
