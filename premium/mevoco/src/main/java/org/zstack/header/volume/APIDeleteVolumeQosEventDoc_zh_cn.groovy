package org.zstack.header.volume

import org.zstack.header.volume.VolumeInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "取消硬盘限速结果"

	ref {
		name "inventory"
		path "org.zstack.header.volume.APISetVolumeQosEvent.inventory"
		desc "目标云盘属性"
		type "VolumeInventory"
		since "3.14.0"
		clz VolumeInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.1.0"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.APIDeleteVolumeQosEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.1.0"
		clz ErrorCode.class
	}
}
