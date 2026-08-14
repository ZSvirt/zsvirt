package org.zstack.imagereplicator

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "镜像服务器与镜像复制组引用清单"

	field {
		name "replicationGroupUuid"
		desc ""
		type "String"
		since "3.5"
	}
	field {
		name "backupStorageUuid"
		desc "镜像存储UUID"
		type "String"
		since "3.5"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.5"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.5"
	}
}
