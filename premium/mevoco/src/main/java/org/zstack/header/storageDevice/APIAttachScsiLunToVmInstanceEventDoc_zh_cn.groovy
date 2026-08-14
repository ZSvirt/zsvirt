package org.zstack.header.storageDevice

import org.zstack.header.errorcode.ErrorCode

doc {

	title "将SCSI Lun加载到虚拟机结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storageDevice.APIAttachScsiLunToVmInstanceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.storageDevice.APIAttachScsiLunToVmInstanceEvent.inventory"
		desc "Scsi Lun清单"
		type "ScsiLunInventory"
		since "3.1.0"
		clz ScsiLunInventory.class
	}
}
