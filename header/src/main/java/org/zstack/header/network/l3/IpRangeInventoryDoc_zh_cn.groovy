package org.zstack.header.network.l3

import java.lang.Integer
import java.lang.Integer
import org.zstack.header.network.l3.IpRangeType
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "l3NetworkUuid"
		desc "三层网络UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "startIp"
		desc "起始地址"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "endIp"
		desc "结束地址"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "netmask"
		desc "掩码"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "prefixLen"
		desc "掩码长度"
		type "String"
		since "zsv 3.1"
	}
	field {
		name "gateway"
		desc "网关"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "networkCidr"
		desc "网络CIDR"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "ipVersion"
		desc "ip协议号"
		type "Integer"
		since "zsv 3.1"
	}
	field {
		name "addressMode"
		desc "IPv6地址分配模式"
		type "String"
		since "zsv 3.1"
	}
	ref {
		name "ipRangeType"
		path "org.zstack.header.network.l3.IpRangeInventory.ipRangeType"
		desc "地址段类型"
		type "IpRangeType"
		since "zsv 3.9"
		clz IpRangeType.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 0.6"
	}
}
