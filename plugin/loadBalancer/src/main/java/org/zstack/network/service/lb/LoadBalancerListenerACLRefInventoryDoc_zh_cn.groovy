package org.zstack.network.service.lb

import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "监听器访问控制策略引用清单"

	field {
		name "id"
		desc "资源唯一标识"
		type "Long"
		since "3.9"
	}
	field {
		name "listenerUuid"
		desc "监听器唯一标识"
		type "String"
		since "3.9"
	}
	field {
		name "aclUuid"
		desc "访问策略组唯一标识"
		type "String"
		since "3.9"
	}
	field {
		name "type"
		desc "访问策略类型"
		type "String"
		since "3.9"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.9"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.9"
	}
}
