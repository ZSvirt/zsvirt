package org.zstack.network.service.lb

import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "负载均衡监听器证书关系清单"

	field {
		name "id"
		desc ""
		type "Long"
		since "2.3"
	}
	field {
		name "listenerUuid"
		desc ""
		type "String"
		since "2.3"
	}
	field {
		name "certificateUuid"
		desc ""
		type "String"
		since "2.3"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.3"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.3"
	}
}
