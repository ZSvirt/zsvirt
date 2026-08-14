package org.zstack.header.storageDevice

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storageDevice.ScsiLunClusterStatusInventory

doc {

	title "检查SCSI Lun与集群连接关系结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storageDevice.APICheckScsiLunClusterStatusReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.storageDevice.APICheckScsiLunClusterStatusReply.inventory"
		desc "null"
		type "ScsiLunClusterStatusInventory"
		since "3.1.0"
		clz ScsiLunClusterStatusInventory.class
	}
}
