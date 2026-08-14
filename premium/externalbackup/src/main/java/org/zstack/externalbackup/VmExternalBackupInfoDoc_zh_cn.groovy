package org.zstack.externalbackup

import org.zstack.externalbackup.VolumeExternalBackupInfo
import java.sql.Timestamp

doc {

	title "云主机外部备份信息"

	field {
		name "liveBackup"
		desc "在线备份"
		type "boolean"
		since "3.9.0"
	}
	ref {
		name "volumes"
		path "org.zstack.externalbackup.VmExternalBackupInfo.volumes"
		desc "云盘信息"
		type "List"
		since "3.9.0"
		clz VolumeExternalBackupInfo.class
	}
	field {
		name "totalSize"
		desc "总大小"
		type "long"
		since "3.9.0"
	}
	field {
		name "uuid"
		desc "云主机的UUID，唯一标示该资源"
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
