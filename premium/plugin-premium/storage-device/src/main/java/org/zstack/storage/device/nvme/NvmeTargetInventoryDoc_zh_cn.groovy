package org.zstack.storage.device.nvme

import org.zstack.storage.device.nvme.NvmeLunInventory
import java.sql.Timestamp

doc {

	title "NVMe设备"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.16.21"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.16.21"
	}
	field {
		name "nqn"
		desc "NVMe 限定名称"
		type "String"
		since "3.16.21"
	}
	field {
		name "nvmeServerUuid"
		desc "nvme服务器UUID"
		type "String"
		since "3.17.21"
	}
	field {
		name "state"
		desc "状态"
		type "String"
		since "3.16.21"
	}
	ref {
		name "nvmeLuns"
		path "org.zstack.storage.device.nvme.NvmeTargetInventory.nvmeLuns"
		desc "null"
		type "List"
		since "3.16.21"
		clz NvmeLunInventory.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.16.21"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.16.21"
	}
}
