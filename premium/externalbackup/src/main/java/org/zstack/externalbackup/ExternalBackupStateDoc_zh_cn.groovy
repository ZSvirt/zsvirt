package org.zstack.externalbackup



doc {

	title "备份状态"

	field {
		name "Creating"
		desc "创建中"
		type "ExternalBackupState"
		since "3.9.0"
	}
	field {
		name "Ready"
		desc "就绪"
		type "ExternalBackupState"
		since "3.9.0"
	}
	field {
		name "Deleting"
		desc "删除中"
		type "ExternalBackupState"
		since "3.9.0"
	}
	field {
		name "InUse"
		desc "使用中"
		type "ExternalBackupState"
		since "3.9.0"
	}
}
