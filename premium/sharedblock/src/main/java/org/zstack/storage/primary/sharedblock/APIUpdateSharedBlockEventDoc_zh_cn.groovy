package org.zstack.storage.primary.sharedblock

import org.zstack.header.errorcode.ErrorCode
import org.zstack.storage.primary.sharedblock.SharedBlockGroupPrimaryStorageInventory

doc {

	title "修改共享块存储中的共享块信息"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.primary.sharedblock.APIUpdateSharedBlockEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.9.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.primary.sharedblock.APIUpdateSharedBlockEvent.inventory"
		desc "共享块存储清单"
		type "SharedBlockGroupPrimaryStorageInventory"
		since "3.9.0"
		clz SharedBlockGroupPrimaryStorageInventory.class
	}
}
