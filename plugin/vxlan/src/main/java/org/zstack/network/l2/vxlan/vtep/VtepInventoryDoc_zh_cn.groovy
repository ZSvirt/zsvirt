package org.zstack.network.l2.vxlan.vtep

import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "VXLAN隧道端点清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "0.6"
	}
	field {
		name "vtepIp"
		desc "隧道端点IP地址"
		type "String"
		since "0.6"
	}
	field {
		name "port"
		desc "端口"
		type "Integer"
		since "0.6"
	}
	field {
		name "type"
		desc "类型"
		type "String"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "poolUuid"
		desc "VXLAN资源池UUID"
		type "String"
		since "0.6"
	}
}
