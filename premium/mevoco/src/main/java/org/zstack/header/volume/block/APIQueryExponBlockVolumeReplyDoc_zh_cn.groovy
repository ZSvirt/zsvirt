package org.zstack.header.volume.block

import org.zstack.header.volume.block.ExponBlockVolumeInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询expon类型块存储卷"

	ref {
		name "inventories"
		path "org.zstack.header.volume.block.APIQueryExponBlockVolumeReply.inventories"
		desc "Expon块存储卷清单列表"
		type "List"
		since "5.1.0"
		clz ExponBlockVolumeInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.block.APIQueryExponBlockVolumeReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.1.0"
		clz ErrorCode.class
	}
}
