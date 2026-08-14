package org.zstack.nas

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "nas挂载点清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.4.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "2.4.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.4.0"
	}
	field {
		name "mountDomain"
		desc "挂载点domain"
		type "String"
		since "2.4.0"
	}
	field {
		name "nasFileSystemUuid"
		desc "nas文件系统UUID"
		type "String"
		since "2.4.0"
	}
	field {
		name "type"
		desc "nas挂载点类型"
		type "String"
		since "2.4.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.4.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.4.0"
	}
}
