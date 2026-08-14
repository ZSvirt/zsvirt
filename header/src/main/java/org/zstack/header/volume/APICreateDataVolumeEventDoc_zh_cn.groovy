package org.zstack.header.volume

import org.zstack.header.errorcode.ErrorCode

doc {

	title "返回创建的云盘清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.APICreateDataVolumeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.volume.APICreateDataVolumeEvent.inventory"
		desc "创建的云盘"
		type "VolumeInventory"
		since "zsv 0.6"
		clz VolumeInventory.class
	}
}
