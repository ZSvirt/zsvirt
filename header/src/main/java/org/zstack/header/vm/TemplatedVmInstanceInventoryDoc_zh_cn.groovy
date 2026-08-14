package org.zstack.header.vm

import java.sql.Timestamp

doc {

	title "虚拟机模板"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 4.2.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 4.2.6"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "zsv 4.2.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 4.2.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 4.2.6"
	}
}
