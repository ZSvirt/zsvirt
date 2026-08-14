package org.zstack.storage.device.fibreChannel

import org.zstack.storage.device.fibreChannel.FiberChannelLunInventory
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "FC SAN存储清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.1.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.1.0"
	}
	field {
		name "wwnn"
		desc "FC SAN全局唯一表示"
		type "String"
		since "3.1.0"
	}
	field {
		name "state"
		desc "启用状态"
		type "String"
		since "3.1.0"
	}
	ref {
		name "fiberChannelLuns"
		path "org.zstack.storage.device.fibreChannel.FiberChannelStorageInventory.fiberChannelLuns"
		desc "FC SAN存储与FC SAN磁盘关联信息"
		type "List"
		since "3.1.0"
		clz FiberChannelLunInventory.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.1.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.1.0"
	}
}
