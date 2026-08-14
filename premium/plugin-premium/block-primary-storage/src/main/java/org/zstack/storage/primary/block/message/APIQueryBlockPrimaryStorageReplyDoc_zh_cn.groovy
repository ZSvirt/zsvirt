package org.zstack.storage.primary.block.message

import org.zstack.storage.primary.block.BlockPrimaryStorageInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询块存储回复"

	ref {
		name "inventories"
		path "org.zstack.storage.primary.block.message.APIQueryBlockPrimaryStorageReply.inventories"
		desc "块存储清单列表"
		type "List"
		since "3.15.11"
		clz BlockPrimaryStorageInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.15.11"
	}
	ref {
		name "error"
		path "org.zstack.storage.primary.block.message.APIQueryBlockPrimaryStorageReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.15.11"
		clz ErrorCode.class
	}
}
