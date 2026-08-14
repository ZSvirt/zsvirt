package org.zstack.header.network.l2

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "二层网络结构"

	field {
		name "uuid"
		desc "资源的 UUID，唯一标示该资源"
		type "String"
		since "zsv 3.10.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 3.10.0"
	}
	field {
		name "poolUuid"
		desc "网络池 UUID"
		type "String"
		since "zsv 3.10.0"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "zsv 3.10.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv 3.10.0"
	}
	field {
		name "physicalInterface"
		desc "网卡名称"
		type "String"
		since "zsv 3.10.0"
	}
	field {
		name "type"
		desc "二层网络类型"
		type "String"
		since "zsv 3.10.0"
	}
	field {
		name "vni"
		desc ""
		type "String"
		since "zsv 3.10.0"
	}
	field {
		name "vlan"
		desc ""
		type "int"
		since "zsv 3.10.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 3.10.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 3.10.0"
	}
}
