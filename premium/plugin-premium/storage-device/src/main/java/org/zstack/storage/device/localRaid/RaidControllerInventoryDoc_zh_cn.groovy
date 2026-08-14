package org.zstack.storage.device.localRaid

import java.sql.Timestamp
import java.sql.Timestamp
import java.lang.Integer
import org.zstack.storage.device.localRaid.RaidPhysicalDriveInventory

doc {

	title "Raid控制器"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.6"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.6"
	}
	field {
		name "productName"
		desc "产品名"
		type "String"
		since "3.6"
	}
	field {
		name "sasAddress"
		desc "SAS地址"
		type "String"
		since "3.6"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "3.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.6"
	}
	field {
		name "adapterNumber"
		desc "Raid卡编好"
		type "Integer"
		since "3.6"
	}
	ref {
		name "raidPhysicalDrives"
		path "org.zstack.storage.device.localRaid.RaidControllerInventory.raidPhysicalDrives"
		desc " 关联的Raid物理盘"
		type "List"
		since "3.6"
		clz RaidPhysicalDriveInventory.class
	}
}
