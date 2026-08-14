package org.zstack.storage.device.iscsi

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "iSCSI服务器集群加载关系清单"

	field {
		name "iscsiServerUuid"
		desc ""
		type "String"
		since "3.0.0"
	}
	field {
		name "clusterUuid"
		desc "集群UUID"
		type "String"
		since "3.0.0"
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
