package org.zstack.storage.device.localRaid

import java.lang.Integer
import java.lang.Integer
import java.lang.Integer
import java.lang.Integer
import java.lang.Long
import org.zstack.storage.device.localRaid.LocateStatus
import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "Raid物理盘清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.6"
	}
	field {
		name "raidLevel"
		desc "Raid级别"
		type "String"
		since "3.6"
	}
	field {
		name "raidControllerUuid"
		desc "Raid控制器Uuid"
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
		name "enclosureDeviceId"
		desc "Enclosure Id"
		type "Integer"
		since "3.6"
	}
	field {
		name "slotNumber"
		desc "槽位号"
		type "Integer"
		since "3.6"
	}
	field {
		name "deviceId"
		desc "Device Id"
		type "Integer"
		since "3.6"
	}
	field {
		name "diskGroup"
		desc "磁盘组 Id"
		type "Integer"
		since "3.6"
	}
	field {
		name "wwn"
		desc "WWN"
		type "String"
		since "3.6"
	}
	field {
		name "serialNumber"
		desc "序列号"
		type "String"
		since "3.6"
	}
	field {
		name "deviceModel"
		desc "磁盘规格"
		type "String"
		since "3.6"
	}
	field {
		name "size"
		desc "磁盘大小"
		type "Long"
		since "3.6"
	}
	field {
		name "driveState"
		desc "磁盘状态"
		type "String"
		since "3.6"
	}
	ref {
		name "locateStatus"
		path "org.zstack.storage.device.localRaid.RaidPhysicalDriveInventory.locateStatus"
		desc "定位灯状态"
		type "LocateStatus"
		since "3.6"
		clz LocateStatus.class
	}
	field {
		name "driveType"
		desc "接口类型"
		type "String"
		since "3.6"
	}
	field {
		name "mediaType"
		desc "介质类型"
		type "String"
		since "3.6"
	}
	field {
		name "rotationRate"
		desc "转速"
		type "Integer"
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
}
