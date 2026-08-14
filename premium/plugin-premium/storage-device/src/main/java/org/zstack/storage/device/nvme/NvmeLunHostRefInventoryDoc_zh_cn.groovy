package org.zstack.storage.device.nvme

import java.sql.Timestamp

doc {

	title "NVMe 磁盘与主机映射关系"

	field {
		name "nvmeLunUuid"
		desc "NVMe磁盘UUID"
		type "String"
		since "3.16.21"
	}
	field {
		name "hostUuid"
		desc "主机 UUID"
		type "String"
		since "3.16.21"
	}
	field {
		name "path"
		desc "磁盘路径"
		type "String"
		since "3.16.21"
	}
	field {
		name "hctl"
		desc "磁盘HCTL"
		type "String"
		since "3.16.21"
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
