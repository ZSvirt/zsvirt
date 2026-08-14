package org.zstack.header.volume

import org.zstack.header.volume.VolumeInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "撤销快照创建的请求返回"

	ref {
		name "inventory"
		path "org.zstack.header.volume.APIUndoSnapshotCreationEvent.inventory"
		desc "硬盘清单"
		type "VolumeInventory"
		since "zsv 3.17.0"
		clz VolumeInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.APIUndoSnapshotCreationEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.17.0"
		clz ErrorCode.class
	}
}
