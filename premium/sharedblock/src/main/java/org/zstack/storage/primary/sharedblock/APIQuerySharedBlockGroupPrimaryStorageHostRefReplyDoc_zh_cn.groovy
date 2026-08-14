package org.zstack.storage.primary.sharedblock

import org.zstack.header.errorcode.ErrorCode
import org.zstack.storage.primary.sharedblock.SharedBlockGroupPrimaryStorageHostRefInventory

doc {

	title "查询共享块设备主存储物理机连接状态结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.primary.sharedblock.APIQuerySharedBlockGroupPrimaryStorageHostRefReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3.2"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.storage.primary.sharedblock.APIQuerySharedBlockGroupPrimaryStorageHostRefReply.inventories"
		desc "null"
		type "List"
		since "2.3.2"
		clz SharedBlockGroupPrimaryStorageHostRefInventory.class
	}
}
