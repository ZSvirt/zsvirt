package org.zstack.header.volume.block

import org.zstack.header.volume.block.BlockVolumeInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询块存储卷返回"

	ref {
		name "inventories"
		path "org.zstack.header.volume.block.APIQueryBlockVolumeReply.inventories"
		desc "查询块存储卷列表"
		type "List"
		since "3.17.11"
		clz BlockVolumeInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.11"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.block.APIQueryBlockVolumeReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.11"
		clz ErrorCode.class
	}
}
