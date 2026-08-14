package org.zstack.header.vpc.ha

import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "高可用组Vip清单"

	field {
		name "id"
		desc ""
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
		name "vipUuid"
		desc "VIP UUID"
		type "String"
		since "3.5"
	}
	field {
		name "l3NetworkUuid"
		desc "三层网络UUID"
		type "String"
		since "3.5"
	}
	field {
		name "ip"
		desc ""
		type "String"
		since "3.5"
	}
	field {
		name "netmask"
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
