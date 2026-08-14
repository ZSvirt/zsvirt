package org.zstack.storage.device.iscsi

import org.zstack.storage.device.iscsi.IscsiLunInventory
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "iSCSI目标清单"

	field {
		name "iscsiServerUuid"
		desc "iSCSI服务器UUID"
		type "String"
		since "3.0.0"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.0.0"
	}
	field {
		name "iqn"
		desc "iSCSI IQN"
		type "String"
		since "3.0.0"
	}
	ref {
		name "iscsiLuns"
		path "org.zstack.storage.device.iscsi.IscsiTargetInventory.iscsiLuns"
		desc "iSCSI磁盘"
		type "List"
		since "3.0.0"
		clz IscsiLunInventory.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.0.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.0.0"
	}
}
