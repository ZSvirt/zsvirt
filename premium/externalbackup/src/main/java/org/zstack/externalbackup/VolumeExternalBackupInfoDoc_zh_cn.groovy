package org.zstack.externalbackup

import java.sql.Timestamp

doc {

	title "云盘外部备份信息"

	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "3.9.0"
	}
	field {
		name "size"
		desc "大小"
		type "long"
		since "3.9.0"
	}
	field {
		name "uuid"
		desc "云盘的UUID，唯一标示该资源"
		type "String"
		since "3.9.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.9.0"
	}
	field {
		name "state"
		desc "状态"
		type "String"
		since "3.9.0"
	}
	field {
		name "installPath"
		desc "路径"
		type "String"
		since "3.9.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.9.0"
	}
}
