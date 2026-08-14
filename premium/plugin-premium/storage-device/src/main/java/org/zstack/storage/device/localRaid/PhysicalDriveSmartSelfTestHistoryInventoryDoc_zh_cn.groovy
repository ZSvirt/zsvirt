package org.zstack.storage.device.localRaid

import java.lang.Long
import org.zstack.storage.device.localRaid.RunningState
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "Raid物理盘自检结果"

	field {
		name "id"
		desc "id"
		type "Long"
		since "3.6"
	}
	field {
		name "raidPhysicalDriveUuid"
		desc "Raid物理盘Uuid"
		type "String"
		since "3.6"
	}
	ref {
		name "runningState"
		path "org.zstack.storage.device.localRaid.PhysicalDriveSmartSelfTestHistoryInventory.runningState"
		desc "自检运行状态"
		type "RunningState"
		since "3.6"
		clz RunningState.class
	}
	field {
		name "testResult"
		desc "自检结果"
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
}
