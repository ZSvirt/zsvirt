package org.zstack.header.storageDevice

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storageDevice.ScsiLunInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取虚拟机可以加载的SCSI Lun结果"

	ref {
		name "error"
		path "org.zstack.header.storageDevice.APIGetScsiLunCandidatesForAttachingVmReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.storageDevice.APIGetScsiLunCandidatesForAttachingVmReply.inventories"
		desc "SCSI Lun清单"
		type "List"
		since "3.1.0"
		clz ScsiLunInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.1.0"
	}
}
