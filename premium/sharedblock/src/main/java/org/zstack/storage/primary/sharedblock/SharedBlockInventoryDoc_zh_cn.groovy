package org.zstack.storage.primary.sharedblock

import org.zstack.storage.primary.sharedblock.SharedBlockType
import org.zstack.storage.primary.sharedblock.SharedBlockState
import org.zstack.storage.primary.sharedblock.SharedBlockStatus
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "共享块设备清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.3.2"
	}
	field {
		name "sharedBlockGroupUuid"
		desc "共享块设备组UUID"
		type "String"
		since "2.3.2"
	}
	ref {
		name "type"
		path "org.zstack.storage.primary.sharedblock.SharedBlockInventory.type"
		desc "共享块设备类型"
		type "SharedBlockType"
		since "2.3.2"
		clz SharedBlockType.class
	}
	field {
		name "diskUuid"
		desc "磁盘唯一标示（例如UUID, WWN, WWID）"
		type "String"
		since "2.3.2"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "2.3.2"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.3.2"
	}
	ref {
		name "state"
		path "org.zstack.storage.primary.sharedblock.SharedBlockInventory.state"
		desc "共享块设备启用状态"
		type "SharedBlockState"
		since "2.3.2"
		clz SharedBlockState.class
	}
	ref {
		name "status"
		path "org.zstack.storage.primary.sharedblock.SharedBlockInventory.status"
		desc "共享块设备连接状态"
		type "SharedBlockStatus"
		since "2.3.2"
		clz SharedBlockStatus.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.3.2"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.3.2"
	}
}
