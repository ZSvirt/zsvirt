package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.PortGroupVlanMode
import java.lang.Integer
import java.lang.Boolean
import java.sql.Timestamp
import org.zstack.header.network.l3.IpRangeInventory
import org.zstack.header.network.service.NetworkServiceL3NetworkRefInventory
import org.zstack.header.network.l3.L3NetworkHostRouteInventory

doc {

	title "端口组清单"

	field {
		name "vSwitchUuid"
		desc "虚拟交换机UUID"
		type "String"
		since "4.2.0"
	}
	ref {
		name "vlanMode"
		path "org.zstack.network.l2.virtualSwitch.header.PortGroupInventory.vlanMode"
		desc "端口组 VLAN 模式"
		type "PortGroupVlanMode"
		since "4.2.0"
		clz PortGroupVlanMode.class
	}
	field {
		name "vlanId"
		desc "Vlan号"
		type "Integer"
		since "4.2.0"
	}
	field {
		name "vlanRanges"
		desc "vlan范围"
		type "String"
		since "4.2.0"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.2.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "4.2.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "4.2.0"
	}
	field {
		name "type"
		desc "三层网络类型"
		type "String"
		since "4.2.0"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "4.2.0"
	}
	field {
		name "l2NetworkUuid"
		desc "二层网络UUID"
		type "String"
		since "4.2.0"
	}
	field {
		name "state"
		desc "状态"
		type "String"
		since "4.2.0"
	}
	field {
		name "dnsDomain"
		desc "DNS域"
		type "String"
		since "4.2.0"
	}
	field {
		name "system"
		desc "是否用于系统云主机"
		type "Boolean"
		since "4.2.0"
	}
	field {
		name "category"
		desc "网络类型，需要与system标签搭配使用，system为true时可设置为Public、Private"
		type "String"
		since "4.2.0"
	}
	field {
		name "ipVersion"
		desc "ip协议号"
		type "Integer"
		since "4.2.0"
	}
	field {
		name "enableIPAM"
		desc "IP地址管理是否启用"
		type "Boolean"
		since "4.2.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.2.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.2.0"
	}
	field {
		name "dns"
		desc ""
		type "List"
		since "4.2.0"
	}
	ref {
		name "ipRanges"
		path "org.zstack.network.l2.virtualSwitch.header.PortGroupInventory.ipRanges"
		desc "IP 范围清单列表"
		type "List"
		since "4.2.0"
		clz IpRangeInventory.class
	}
	ref {
		name "networkServices"
		path "org.zstack.network.l2.virtualSwitch.header.PortGroupInventory.networkServices"
		desc "网络服务与三层网络映射清单列表"
		type "List"
		since "4.2.0"
		clz NetworkServiceL3NetworkRefInventory.class
	}
	ref {
		name "hostRoute"
		path "org.zstack.network.l2.virtualSwitch.header.PortGroupInventory.hostRoute"
		desc "主机路由清单列表"
		type "List"
		since "4.2.0"
		clz L3NetworkHostRouteInventory.class
	}
}
