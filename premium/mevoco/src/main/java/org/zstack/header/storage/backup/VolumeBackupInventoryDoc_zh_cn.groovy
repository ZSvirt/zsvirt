package org.zstack.header.storage.backup

import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.header.storage.backup.VolumeBackupStorageRefInventory

doc {

	title "卷备份清单"

	field {
		name "uuid"
		desc "卷备份的UUID，唯一标示该资源"
		type "String"
		since "2.6"
	}
	field {
		name "volumeUuid"
		desc "云盘UUID"
		type "String"
		since "2.6"
	}
	field {
		name "name"
		desc "备份名称"
		type "String"
		since "2.6"
	}
	field {
		name "description"
		desc "备份的详细描述"
		type "String"
		since "2.6"
	}
	field {
		name "type"
		desc "卷的类型"
		type "String"
		since "2.6"
	}
	field {
		name "state"
		desc "卷备份的启用状态"
		type "String"
		since "2.6"
	}
	field {
		name "status"
		desc "卷备份的状态"
		type "String"
		since "2.6"
	}
	field {
		name "size"
		desc "当前卷备份的大小"
		type "Long"
		since "2.6"
	}
	field {
		name "metadata"
		desc "卷备份相关元数据"
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
	ref {
		name "backupStorageRefs"
		path "org.zstack.header.storage.backup.VolumeBackupInventory.backupStorageRefs"
		desc "卷备份所在备份服务器列表"
		type "List"
		since "2.6"
		clz VolumeBackupStorageRefInventory.class
	}
}
