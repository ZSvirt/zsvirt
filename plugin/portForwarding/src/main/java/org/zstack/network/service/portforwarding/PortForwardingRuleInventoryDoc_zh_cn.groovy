package org.zstack.network.service.portforwarding

import java.lang.Integer
import java.lang.Integer
import java.lang.Integer
import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "端口转发规则清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "vipIp"
		desc "VIP的IP地址"
		type "String"
		since "0.6"
	}
	field {
		name "guestIp"
		desc "虚拟机网卡的IP地址"
		type "String"
		since "0.6"
	}
	field {
		name "vipUuid"
		desc "VIP UUID"
		type "String"
		since "0.6"
	}
	field {
		name "vipPortStart"
		desc "VIP的起始端口号"
		type "Integer"
		since "0.6"
	}
	field {
		name "vipPortEnd"
		desc "VIP的结束端口号"
		type "Integer"
		since "0.6"
	}
	field {
		name "privatePortStart"
		desc "客户IP的起始端口号"
		type "Integer"
		since "0.6"
	}
	field {
		name "privatePortEnd"
		desc "客户IP的结束端口号"
		type "Integer"
		since "0.6"
	}
	field {
		name "vmNicUuid"
		desc "云主机网卡UUID"
		type "String"
		since "0.6"
	}
	field {
		name "protocolType"
		desc "网络流量的协议类型"
		type "String"
		since "0.6"
	}
	field {
		name "state"
		desc "规则可用状态"
		type "String"
		since "0.6"
	}
	field {
		name "allowedCidr"
		desc "源CIDR; 端口转发规则只作用于源CIDR的流量"
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
}
