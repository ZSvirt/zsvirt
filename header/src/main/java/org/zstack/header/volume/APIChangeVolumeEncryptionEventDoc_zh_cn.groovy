package org.zstack.header.volume

import org.zstack.header.volume.VolumeInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "转换硬盘加密属性"

	ref {
		name "inventory"
		path "org.zstack.header.volume.APIChangeVolumeEncryptionEvent.inventory"
		desc "云盘清单"
		type "VolumeInventory"
		since "1.0.1, zsv_5.1.0"
		clz VolumeInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "1.0.1, zsv_5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.APIChangeVolumeEncryptionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "1.0.1, zsv_5.1.0"
		clz ErrorCode.class
	}
}
