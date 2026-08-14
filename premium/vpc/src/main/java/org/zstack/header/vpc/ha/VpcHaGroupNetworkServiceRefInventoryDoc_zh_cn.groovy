package org.zstack.header.vpc.ha

import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "高可用组网络资源清单"

	field {
		name "id"
		desc "唯一标示该资源"
		type "Long"
		since "3.5"
	}
	field {
		name "vpcHaRouterUuid"
		desc ""
		type "String"
		since "3.5"
	}
	field {
		name "networkServiceName"
		desc ""
		type "String"
		since "3.5"
	}
	field {
		name "networkServiceUuid"
		desc ""
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
