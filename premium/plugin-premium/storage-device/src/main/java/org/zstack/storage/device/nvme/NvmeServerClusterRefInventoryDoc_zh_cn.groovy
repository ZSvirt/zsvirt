package org.zstack.storage.device.nvme

import java.sql.Timestamp

doc {

	title "NVMe服务器集群加载关系清单"

	field {
		name "nvmeServerUuid"
		desc "NVMe 服务器 UUID"
		type "String"
		since "3.17.21"
	}
	field {
		name "clusterUuid"
		desc "集群 UUID"
		type "String"
		since "3.17.21"
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
