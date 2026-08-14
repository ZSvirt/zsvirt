package org.zstack.storage.device.localRaid

import org.zstack.header.errorcode.ErrorCode
import org.zstack.storage.device.localRaid.RaidPhysicalDriveInventory

doc {

	title "操作Raid物理盘定位灯结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.device.localRaid.APILocateLocalRaidPhysicalDriveEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.device.localRaid.APILocateLocalRaidPhysicalDriveEvent.inventory"
		desc "null"
		type "RaidPhysicalDriveInventory"
		since "3.6"
		clz RaidPhysicalDriveInventory.class
	}
}
