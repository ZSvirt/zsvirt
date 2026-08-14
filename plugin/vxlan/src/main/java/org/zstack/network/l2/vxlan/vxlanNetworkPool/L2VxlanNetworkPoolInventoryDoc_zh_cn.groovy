package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.network.l2.vxlan.vtep.VtepInventory
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VniRangeInventory
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "VXLAN资源池清单"

	ref {
		name "attachedVtepRefs"
		path "org.zstack.network.l2.vxlan.vxlanNetworkPool.L2VxlanNetworkPoolInventory.attachedVtepRefs"
		desc "null"
		type "List"
		since "0.6"
		clz VtepInventory.class
	}
	ref {
		name "attachedVxlanNetworkRefs"
		path "org.zstack.network.l2.vxlan.vxlanNetworkPool.L2VxlanNetworkPoolInventory.attachedVxlanNetworkRefs"
		desc "null"
		type "List"
		since "0.6"
		clz L2VxlanNetworkInventory.class
	}
	ref {
		name "attachedVniRanges"
		path "org.zstack.network.l2.vxlan.vxlanNetworkPool.L2VxlanNetworkPoolInventory.attachedVniRanges"
		desc "null"
		type "List"
		since "0.6"
		clz VniRangeInventory.class
	}
	field {
		name "attachedCidrs"
		desc "已加载CIDR映射表"
		type "Map"
		since "0.6"
	}
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
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "0.6"
	}
	field {
		name "physicalInterface"
		desc "物理网卡"
		type "String"
		since "0.6"
	}
	field {
		name "type"
		desc "二层网络类型"
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
	field {
		name "attachedClusterUuids"
		desc "挂载集群的UUID列表"
		type "List"
		since "0.6"
	}
}
