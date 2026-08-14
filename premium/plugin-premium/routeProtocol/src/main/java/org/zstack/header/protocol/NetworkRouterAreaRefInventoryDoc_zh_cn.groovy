package org.zstack.header.protocol

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.4"
	}
	field {
		name "vRouterUuid"
		desc ""
		type "String"
		since "3.4"
	}
	field {
		name "applianceVmType"
		desc ""
		type "String"
		since "3.4"
	}
	field {
		name "routerAreaUuid"
		desc ""
		type "String"
		since "3.4"
	}
	field {
		name "l3NetworkUuid"
		desc "三层网络UUID"
		type "String"
		since "3.4"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.4"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.4"
	}
}
