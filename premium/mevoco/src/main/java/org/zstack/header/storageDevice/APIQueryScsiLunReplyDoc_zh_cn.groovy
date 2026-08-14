package org.zstack.header.storageDevice

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storageDevice.ScsiLunInventory

doc {

	title "查询 SCSI Lun 结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storageDevice.APIQueryScsiLunReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.2.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.storageDevice.APIQueryScsiLunReply.inventories"
		desc "null"
		type "List"
		since "3.2.0"
		clz ScsiLunInventory.class
	}
}
