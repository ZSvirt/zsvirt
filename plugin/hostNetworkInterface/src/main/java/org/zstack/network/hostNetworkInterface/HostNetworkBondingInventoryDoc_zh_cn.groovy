package org.zstack.network.hostNetworkInterface

doc {

	title "物理机Bond设备清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.5.0"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "3.5.0"
	}
	field {
		name "bondingName"
		desc "Bond名称"
		type "String"
		since "3.5.0"
	}
	field {
		name "bondingType"
		desc "Bond应用状态，有noBridge、bridgeSlave"
		type "String"
		since "3.17.0"
	}
	field {
		name "speed"
		desc "Bond速率"
		type "Long"
		since "3.17.0"
	}
	field {
		name "mode"
		desc "Bond模式"
		type "String"
		since "3.5.0"
	}
	field {
		name "xmitHashPolicy"
		desc "哈希策略"
		type "String"
		since "3.5.0"
	}
	field {
		name "miiStatus"
		desc "mii状态"
		type "String"
		since "3.5.0"
	}
	field {
		name "mac"
		desc "MAC地址"
		type "String"
		since "3.5.0"
	}
	field {
		name "ipAddresses"
		desc "IP地址"
		type "List"
		since "3.5.0"
	}
	field {
		name "gateway"
		desc "网关地址"
		type "String"
		since "3.17.0"
	}
	field {
		name "callBackIp"
		desc "回调地址"
		type "String"
		since "3.17.0"
	}
	field {
		name "miimon"
		desc "mii监控间隔"
		type "Long"
		since "3.5.0"
	}
	field {
		name "type"
		desc "Bond类型"
		type "String"
		since "3.17.0"
	}
	field {
		name "allSlavesActive"
		desc ""
		type "Boolean"
		since "3.5.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.17.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.5.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.5.0"
	}
	ref {
		name "slaves"
		path "org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory.slaves"
		desc "BOND Slaves"
		type "List"
		since "3.5.0"
		clz HostNetworkInterfaceInventory.class
	}
}
