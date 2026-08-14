package org.zstack.header.storage.backup

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "卷备份索引清单"

	field {
		name "volumeBackupUuid"
		desc "卷备份的uuid"
		type "String"
		since "2.6"
	}
	field {
		name "backupStorageUuid"
		desc "镜像存储UUID"
		type "String"
		since "2.6"
	}
	field {
		name "installPath"
		desc "卷备份的数据路径"
		type "String"
		since "2.6"
	}
	field {
		name "status"
		desc "卷备份在镜像存储的状态"
		type "String"
		since "2.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.6"
	}
}
