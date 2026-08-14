package org.zstack.network.l2.virtualSwitch.header

import java.lang.Integer
import java.lang.Long
import java.sql.Timestamp

doc {

	title "Kernel适配器使用的ip清单"

	field {
		name "hostKernelInterfaceUuid"
		desc "Kernel适配器的UUID"
		type "String"
		since "4.1.0"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.1.0"
	}
	field {
		name "ipRangeUuid"
		desc "IP段UUID"
		type "String"
		since "4.1.0"
	}
	field {
		name "l3NetworkUuid"
		desc "三层网络UUID"
		type "String"
		since "4.1.0"
	}
	field {
		name "ipVersion"
		desc "IP协议号"
		type "Integer"
		since "4.1.0"
	}
	field {
		name "ip"
		desc "IP地址"
		type "String"
		since "4.1.0"
	}
	field {
		name "netmask"
		desc "子网掩码"
		type "String"
		since "4.1.0"
	}
	field {
		name "gateway"
		desc "网关地址"
		type "String"
		since "4.1.0"
	}
	field {
		name "usedFor"
		desc "分配原因"
		type "String"
		since "4.1.0"
	}
	field {
		name "ipInLong"
		desc "Long存储的IP地址，仅ipv4有效"
		type "Long"
		since "4.1.0"
	}
	field {
		name "vmNicUuid"
		desc "云主机网卡UUID"
		type "String"
		since "4.1.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.1.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.1.0"
	}
}
