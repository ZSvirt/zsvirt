package org.zstack.header.network.l3

import java.lang.Integer
import java.sql.Timestamp

doc {

	title "保留地址段清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "l3NetworkUuid"
		desc "三层网络UUID"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "startIp"
		desc "起始IP(包含在保留地址段内)"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "endIp"
		desc "结束IP(包含在保留地址段内)"
		type "String"
		since "zsv 4.10.16"
	}
	field {
		name "ipVersion"
		desc "ip协议号"
		type "Integer"
		since "zsv 4.10.16"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 4.10.16"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 4.10.16"
	}
}
