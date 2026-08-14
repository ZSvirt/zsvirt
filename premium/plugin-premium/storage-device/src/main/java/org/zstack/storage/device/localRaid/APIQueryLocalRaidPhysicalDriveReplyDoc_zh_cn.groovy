package org.zstack.storage.device.localRaid

import org.zstack.header.errorcode.ErrorCode
import org.zstack.storage.device.localRaid.RaidPhysicalDriveInventory

doc {

	title "查询Raid物理盘"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.device.localRaid.APIQueryLocalRaidPhysicalDriveReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.storage.device.localRaid.APIQueryLocalRaidPhysicalDriveReply.inventories"
		desc "null"
		type "List"
		since "3.6"
		clz RaidPhysicalDriveInventory.class
	}
}
