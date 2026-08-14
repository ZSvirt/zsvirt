package org.zstack.network.hostNetworkInterface.lldp.entity

doc {

	title "物理网卡lldp信息清单"

	field {
		name "lldpUuid"
		desc "Lldp UUID"
		type "String"
		since "4.1.0"
	}
	field {
		name "chassisId"
		desc "机箱标识"
		type "String"
		since "4.1.0"
	}
	field {
		name "timeToLive"
		desc "生存时间"
		type "Integer"
		since "4.1.0"
	}
	field {
		name "managementAddress"
		desc "管理地址"
		type "String"
		since "4.1.0"
	}
	field {
		name "systemName"
		desc "系统名称"
		type "String"
		since "4.1.0"
	}
	field {
		name "systemDescription"
		desc "系统描述"
		type "String"
		since "4.1.0"
	}
	field {
		name "systemCapabilities"
		desc "系统能力"
		type "String"
		since "4.1.0"
	}
	field {
		name "portId"
		desc "端口标识"
		type "String"
		since "4.1.0"
	}
	field {
		name "portDescription"
		desc "端口描述"
		type "String"
		since "4.1.0"
	}
	field {
		name "vlanId"
		desc "VLAN标识"
		type "Integer"
		since "4.1.0"
	}
	field {
		name "aggregationPortId"
		desc "聚合端口标识"
		type "Long"
		since "4.1.0"
	}
	field {
		name "mtu"
		desc "最大传输单元"
		type "Integer"
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
