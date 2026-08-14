package org.zstack.header.baremetal.network

doc {

	title "裸机网络配置"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.6.0"
	}
	field {
		name "baremetalInstanceUuid"
		desc "裸机实例UUID"
		type "String"
		since "2.6.0"
	}
	field {
		name "l3NetworkUuid"
		desc "三层网络UUID"
		type "String"
		since "2.6.0"
	}
	field {
		name "baremetalBondingUuid"
		desc "BOND UUID"
		type "String"
		since "3.4.0"
	}
	field {
		name "mac"
		desc "网卡MAC"
		type "String"
		since "2.6.0"
	}
	field {
		name "ip"
		desc "IP地址"
		type "String"
		since "2.6.0"
	}
	field {
		name "netmask"
		desc "子网掩码"
		type "String"
		since "2.6.0"
	}
	field {
		name "gateway"
		desc "网关"
		type "String"
		since "2.6.0"
	}
	field {
		name "metadata"
		desc ""
		type "String"
		since "2.6.0"
	}
	field {
		name "pxe"
		desc "是否PXE启动网卡"
		type "Boolean"
		since "2.6.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.6.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.6.0"
	}
}
