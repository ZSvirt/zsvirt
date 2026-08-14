package org.zstack.network.l2.virtualSwitch.header

import java.lang.Boolean
import org.zstack.network.l2.virtualSwitch.header.L2PortGroupNetworkInventory
import java.lang.Integer
import java.sql.Timestamp

doc {

	title "虚拟交换机清单"

	field {
		name "isDistributed"
		desc "是否为分布式"
		type "Boolean"
		since "3.17.0"
	}
	ref {
		name "portGroups"
		path "org.zstack.network.l2.virtualSwitch.header.L2VirtualSwitchNetworkInventory.portGroups"
		desc "端口组列表清单"
		type "List"
		since "3.17.0"
		clz L2PortGroupNetworkInventory.class
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
