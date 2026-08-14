package org.zstack.header.baremetal.pxeserver

doc {

	title "裸机部署服务器清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.1.1"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "3.1.1"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.1.1"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.1.1"
	}
	field {
		name "hostname"
		desc "部署服务器地址"
		type "String"
		since "3.1.1"
	}
	field {
		name "sshUsername"
		desc "部署服务器SSH账户"
		type "String"
		since "3.1.1"
	}
	field {
		name "sshPassword"
		desc "部署服务器SSH密码"
		type "String"
		since "3.1.1"
	}
	field {
		name "sshPort"
		desc "部署服务器SSH端口"
		type "Integer"
		since "3.1.1"
	}
	field {
		name "storagePath"
		desc "部署服务器存储路径"
		type "String"
		since "3.1.1"
	}
	field {
		name "dhcpInterface"
		desc "DHCP请求监听网卡"
		type "String"
		since "3.1.1"
	}
	field {
		name "dhcpInterfaceAddress"
		desc "DHCP请求监听网卡IP"
		type "String"
		since "3.1.1"
	}
	field {
		name "dhcpRangeBegin"
		desc "DHCP地址范围起始"
		type "String"
		since "3.1.1"
	}
	field {
		name "dhcpRangeEnd"
		desc "DHCP地址范围终止"
		type "String"
		since "3.1.1"
	}
	field {
		name "dhcpRangeNetmask"
		desc "DHCP子网掩码"
		type "String"
		since "3.1.1"
	}
	field {
		name "state"
		desc "部署服务器运行状态"
		type "String"
		since "3.1.1"
	}
	field {
		name "status"
		desc "部署服务器连接状态"
		type "String"
		since "3.1.1"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.1.1"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.1.1"
	}
	field {
		name "totalCapacity"
		desc "存储路径总容量"
		type "Long"
		since "3.1.1"
	}
	field {
		name "availableCapacity"
		desc "存储路径可用容量"
		type "Long"
		since "3.1.1"
	}
	field {
		name "attachedClusterUuids"
		desc "部署服务器挂载集群UUID列表"
		type "List"
		since "3.1.1"
	}
}
