package org.zstack.storage.device.nvme

import java.lang.Integer
import org.zstack.storage.device.nvme.NvmeTargetInventory
import org.zstack.storage.device.nvme.NvmeServerClusterRefInventory
import java.sql.Timestamp

doc {

	title "NVNe服务器清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.17.21"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.17.21"
	}
	field {
		name "ip"
		desc ""
		type "String"
		since "3.17.21"
	}
	field {
		name "port"
		desc ""
		type "Integer"
		since "3.17.21"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "3.17.21"
	}
	field {
		name "transport"
		desc ""
		type "String"
		since "3.17.21"
	}
	ref {
		name "nvmeTargets"
		path "org.zstack.storage.device.nvme.NvmeServerInventory.nvmeTargets"
		desc "null"
		type "List"
		since "3.17.21"
		clz NvmeTargetInventory.class
	}
	ref {
		name "nvmeClusterRefs"
		path "org.zstack.storage.device.nvme.NvmeServerInventory.nvmeClusterRefs"
		desc "null"
		type "List"
		since "3.17.21"
		clz NvmeServerClusterRefInventory.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.17.21"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.17.21"
	}
}
