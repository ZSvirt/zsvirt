package org.zstack.network.hostNetworkInterface.lldp.entity

doc {

	title "物理网卡lldp配置清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.1.0"
	}
	field {
		name "interfaceUuid"
		desc "物理网口Uuid"
		type "String"
		since "4.1.0"
	}
	field {
		name "mode"
		desc "lldp工作模式"
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
	ref {
		name "lldp"
		path "org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpInventory.neighborDevice"
		desc "物理网口lldp信息清单"
		type "HostNetworkInterfaceLldpRefInventory"
		since "4.1.0"
		clz HostNetworkInterfaceLldpRefInventory.class
	}
}
