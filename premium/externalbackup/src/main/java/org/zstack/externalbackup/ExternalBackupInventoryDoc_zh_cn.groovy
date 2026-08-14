package org.zstack.externalbackup

import org.zstack.externalbackup.ExternalBackupState
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "外部备份清单"

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
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.9.0"
	}
	ref {
		name "state"
		path "org.zstack.externalbackup.ExternalBackupInventory.state"
		desc "状态"
		type "ExternalBackupState"
		since "3.9.0"
		clz ExternalBackupState.class
	}
	field {
		name "installPath"
		desc "路径"
		type "String"
		since "3.9.0"
	}
	field {
		name "totalSize"
		desc "总大小"
		type "long"
		since "3.9.0"
	}
	field {
		name "version"
		desc "版本"
		type "String"
		since "3.9.0"
	}
	field {
		name "type"
		desc "类型"
		type "String"
		since "3.9.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.9.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.9.0"
	}
}
