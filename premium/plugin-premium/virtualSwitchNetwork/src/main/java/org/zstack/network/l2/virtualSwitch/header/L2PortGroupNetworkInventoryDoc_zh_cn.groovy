package org.zstack.network.l2.virtualSwitch.header

doc {

    title "端口组二层网络清单"

	field {
		name "vSwitchUuid"
		desc "虚拟交换机UUID"
		type "String"
		since "3.17.0"
	}
	ref {
		name "vlanMode"
		path "org.zstack.network.l2.virtualSwitch.header.L2PortGroupNetworkInventory.vlanMode"
		desc "VLAN 模式"
		type "PortGroupVlanMode"
		since "3.17.0"
		clz PortGroupVlanMode.class
	}
	field {
		name "vlanId"
		desc "vlanID"
		type "Integer"
		since "3.17.0"
	}
	field {
		name "vlanRanges"
		desc "vlan范围"
		type "String"
		since "3.17.0"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.17.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.17.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.17.0"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "3.17.0"
	}
	field {
		name "physicalInterface"
		desc "物理网卡"
		type "String"
		since "3.17.0"
	}
	field {
		name "type"
		desc "二层网络类型"
		type "String"
		since "3.17.0"
	}
	field {
		name "vSwitchType"
		desc "虚拟交换机类型"
		type "String"
		since "3.17.0"
	}
	field {
		name "virtualNetworkId"
		desc "虚拟网络ID"
		type "Integer"
		since "3.17.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.17.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.17.0"
	}
	field {
		name "attachedClusterUuids"
		desc "挂载集群的UUID列表"
		type "List"
		since "3.17.0"
	}
}
