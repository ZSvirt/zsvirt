package org.zstack.header.network.l3

import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "主机路由清单"

	field {
		name "id"
		desc ""
		type "Long"
		since "zsv 2.3"
	}
	field {
		name "l3NetworkUuid"
		desc "三层网络UUID"
		type "String"
		since "zsv 2.3"
	}
	field {
		name "prefix"
		desc ""
		type "String"
		since "zsv 2.3"
	}
	field {
		name "nexthop"
		desc ""
		type "String"
		since "zsv 2.3"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 2.3"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 2.3"
	}
}
