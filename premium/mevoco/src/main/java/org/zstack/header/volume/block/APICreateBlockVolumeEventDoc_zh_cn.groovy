package org.zstack.header.volume.block

import org.zstack.header.volume.block.BlockVolumeInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建块存储卷的返回"

	ref {
		name "inventory"
		path "org.zstack.header.volume.block.APICreateBlockVolumeEvent.inventory"
		desc "块存储卷清单"
		type "BlockVolumeInventory"
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
		path "org.zstack.header.volume.block.APICreateBlockVolumeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.11"
		clz ErrorCode.class
	}
}
