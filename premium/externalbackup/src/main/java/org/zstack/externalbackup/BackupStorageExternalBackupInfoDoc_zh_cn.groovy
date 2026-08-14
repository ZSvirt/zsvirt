package org.zstack.externalbackup

import java.sql.Timestamp

doc {

	title "镜像服务器外部备份信息"

	field {
		name "size"
		desc "大小"
		type "long"
		since "3.9.0"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
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
		desc "备份路径"
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
