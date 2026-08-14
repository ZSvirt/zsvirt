package org.zstack.mevoco

import org.zstack.header.errorcode.ErrorCode
import org.zstack.mevoco.ShareableVolumeVmInstanceRefInventory

doc {

	title "获取到的云主机共享云盘清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.mevoco.APIQueryShareableVolumeVmInstanceRefReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.mevoco.APIQueryShareableVolumeVmInstanceRefReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz ShareableVolumeVmInstanceRefInventory.class
	}
}
